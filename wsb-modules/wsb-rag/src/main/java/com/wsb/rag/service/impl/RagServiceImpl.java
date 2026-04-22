package com.wsb.rag.service.impl;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.config.RagRecommendProperties;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.VectorService;
import com.wsb.rag.util.ClcCategoryUtils;
import com.wsb.rag.util.QueryTextAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final int QUERY_RRF_RANK_CONSTANT = 60;

    private final RemoteBookService remoteBookService;
    private final VectorService vectorService;
    private final RabbitTemplate rabbitTemplate;
    private final RagRecommendProperties recommendProperties;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Override
    public List<BookRemoteDTO> recommend(String query, int limit, Long ownerId) {
        if (StringUtils.isBlank(query) || limit <= 0) {
            return List.of();
        }

        int candidateLimit = resolveRecommendCandidateLimit(limit);
        List<String> expandedQueries = QueryTextAnalyzer.expandQueries(
                query, recommendProperties.getMaxExpandedQueries());
        List<Long> bookIds = ownerId == null
                ? searchExpandedQueries(expandedQueries, candidateLimit, Set.of())
                : searchOwnedBooks(expandedQueries, candidateLimit, ownerId);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        List<BookRemoteDTO> candidates = fetchBooksInOrder(bookIds);
        return rerankBooks(query, candidates, limit);
    }

    @Override
    public List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit) {
        List<Long> bookIds = vectorService.getSimilarBooks(bookId, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        return fetchBooksInOrder(bookIds);
    }

    @Override
    public void enqueueSummary(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, summaryRoutingKey, bookId);
        log.info("已发送摘要生成任务: bookId={}", bookId);
    }

    private List<Long> searchOwnedBooks(List<String> queries, int limit, Long ownerId) {
        List<Long> ownedBookIds = remoteBookService.getBookIdsByOwner(ownerId).getData();
        if (ownedBookIds == null || ownedBookIds.isEmpty()) {
            return List.of();
        }

        Set<Long> ownedBookIdSet = ownedBookIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        if (ownedBookIdSet.isEmpty()) {
            return List.of();
        }
        return searchExpandedQueries(queries, limit, ownedBookIdSet);
    }

    private List<Long> searchExpandedQueries(List<String> queries, int limit, Set<Long> bookIdFilter) {
        if (queries == null || queries.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Integer> bestRanks = new HashMap<>();
        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            String expandedQuery = queries.get(queryIndex);
            double queryWeight = queryIndex == 0 ? 1.0 : 0.75;
            List<Long> rankedIds = vectorService.searchSimilar(expandedQuery, limit, bookIdFilter);
            for (int rank = 0; rank < rankedIds.size(); rank++) {
                Long bookId = rankedIds.get(rank);
                if (bookId == null) {
                    continue;
                }
                scores.merge(bookId, queryWeight / (QUERY_RRF_RANK_CONSTANT + rank + 1), Double::sum);
                bestRanks.merge(bookId, rank, Math::min);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> bestRanks.getOrDefault(entry.getKey(), Integer.MAX_VALUE))
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<BookRemoteDTO> fetchBooksInOrder(List<Long> bookIds) {
        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        List<BookRemoteDTO> books = result.getData();
        if (books == null || books.isEmpty()) {
            return List.of();
        }

        Map<Long, BookRemoteDTO> booksById = books.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        BookRemoteDTO::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return bookIds.stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<BookRemoteDTO> rerankBooks(String query, List<BookRemoteDTO> books, int limit) {
        if (books.isEmpty()) {
            return List.of();
        }

        return scoreBooksLocally(query, books).stream()
                .map(ScoredBook::book)
                .limit(limit)
                .toList();
    }

    private List<ScoredBook> scoreBooksLocally(String query, List<BookRemoteDTO> books) {
        List<String> terms = extractQueryTerms(query);
        List<ScoredBook> scoredBooks = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            BookRemoteDTO book = books.get(i);
            scoredBooks.add(new ScoredBook(book, calculateLocalScore(query, terms, book, i), i));
        }

        return scoredBooks.stream()
                .sorted(Comparator
                        .comparingDouble(ScoredBook::score)
                        .reversed()
                        .thenComparingInt(ScoredBook::originalRank))
                .toList();
    }

    private double calculateLocalScore(String query, List<String> terms, BookRemoteDTO book, int originalRank) {
        double score = 1.0 / (originalRank + 1);
        score += calculateFieldScore(query, terms, book.getTitle(), 6.0);
        score += calculateFieldScore(query, terms, book.getAuthor(), 5.0);
        score += calculateFieldScore(query, terms, book.getKeyword(), 4.0);
        score += calculateFieldScore(query, terms, ClcCategoryUtils.resolveCategory(book.getClc()), 3.0);
        score += calculateFieldScore(query, terms, book.getClc(), 2.5);
        score += calculateFieldScore(query, terms, book.getSummary(), 1.0);
        return score;
    }

    private double calculateFieldScore(String query, List<String> terms, String fieldValue, double weight) {
        if (StringUtils.isBlank(fieldValue)) {
            return 0.0;
        }

        String normalizedField = fieldValue.toLowerCase(Locale.ROOT);
        String normalizedQuery = StringUtils.defaultString(query).trim().toLowerCase(Locale.ROOT);
        double score = 0.0;
        if (StringUtils.isNotBlank(normalizedQuery)) {
            if (normalizedField.equals(normalizedQuery)) {
                score += weight * 3.0;
            } else if (normalizedField.contains(normalizedQuery)) {
                score += weight * 2.0;
            }
        }

        for (String term : terms) {
            if (StringUtils.isNotBlank(term) && normalizedField.contains(term)) {
                score += weight;
            }
        }
        return score;
    }

    private List<String> extractQueryTerms(String query) {
        List<String> terms = QueryTextAnalyzer.extractTerms(query, 12);
        if (!terms.isEmpty()) {
            return terms;
        }
        String normalized = QueryTextAnalyzer.normalize(query).toLowerCase(Locale.ROOT);
        return StringUtils.isBlank(normalized) ? List.of() : List.of(normalized);
    }

    private int resolveRecommendCandidateLimit(int limit) {
        int safeLimit = Math.max(limit, 1);
        int multiplier = Math.max(recommendProperties.getCandidateMultiplier(), 1);
        return Math.max(recommendProperties.getMinCandidates(), safeLimit * multiplier);
    }

    private record ScoredBook(BookRemoteDTO book, double score, int originalRank) {
    }
}
