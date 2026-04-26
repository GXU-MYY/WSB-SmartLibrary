package com.wsb.rag.service.impl;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.config.RagRecommendProperties;
import com.wsb.rag.dto.RecommendedBookPreviewDTO;
import com.wsb.rag.service.BookAiContentService;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.VectorService;
import com.wsb.rag.util.QueryTextAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
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
    private static final int DEFAULT_DLQ_REQUEUE_LIMIT = 20;
    private static final int MAX_DLQ_REQUEUE_LIMIT = 100;

    private final RemoteBookService remoteBookService;
    private final VectorService vectorService;
    private final RabbitTemplate rabbitTemplate;
    private final RagRecommendProperties recommendProperties;
    private final BookAiContentService bookAiContentService;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Value("${rag.queue.summary}")
    private String summaryQueue;

    @Value("${rag.queue.embedding}")
    private String embeddingQueue;

    @Override
    public List<BookRemoteDTO> recommend(String query, int limit, Long ownerId) {
        if (StringUtils.isBlank(query) || limit <= 0) {
            return List.of();
        }

        List<String> expandedQueries = QueryTextAnalyzer.expandQueries(
                query, recommendProperties.getMaxExpandedQueries());
        List<Long> bookIds = ownerId == null
                ? searchExpandedQueries(expandedQueries, limit, Set.of())
                : searchOwnedBooks(expandedQueries, limit, ownerId);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        List<BookRemoteDTO> candidates = fetchBooksInOrder(bookIds);
        return candidates.stream()
                .limit(limit)
                .toList();
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
    public RecommendedBookPreviewDTO getRecommendedBookPreview(Long bookId) {
        if (bookId == null) {
            return null;
        }

        Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
        BookRemoteDTO book = result.getData();
        if (book == null) {
            return null;
        }

        RecommendedBookPreviewDTO preview = new RecommendedBookPreviewDTO();
        preview.setId(book.getId());
        preview.setTitle(book.getTitle());
        preview.setCoverUrl(book.getCoverUrl());
        preview.setAuthor(book.getAuthor());
        preview.setPublisher(book.getPublisher());
        preview.setIsbn(StringUtils.defaultIfBlank(book.getIsbn(), book.getIsbn10()));
        preview.setSummary(book.getSummary());
        preview.setReviewDigest(bookAiContentService.getCachedReviewDigest(bookId));
        return preview;
    }

    @Override
    public void enqueueSummary(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, summaryRoutingKey, bookId);
        log.info("已发送摘要生成任务: bookId={}", bookId);
    }

    @Override
    public void enqueueEmbedding(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
        log.info("已发送向量生成任务: bookId={}", bookId);
    }

    @Override
    public int requeueDeadLetters(String taskType, int limit) {
        DeadLetterTarget target = resolveDeadLetterTarget(taskType);
        int safeLimit = resolveDeadLetterRequeueLimit(limit);
        int count = 0;
        for (int i = 0; i < safeLimit; i++) {
            Object payload = rabbitTemplate.receiveAndConvert(target.queueName());
            if (payload == null) {
                break;
            }
            rabbitTemplate.convertAndSend(exchange, target.routingKey(), payload);
            count++;
        }
        log.info("已重投 RAG 死信消息: taskType={}, count={}", taskType, count);
        return count;
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

        int candidateLimit = resolveRecommendCandidateLimit(limit);
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Integer> bestRanks = new HashMap<>();
        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            String expandedQuery = queries.get(queryIndex);
            double queryWeight = queryIndex == 0 ? 1.0 : 0.75;
            List<Long> rankedIds = vectorService.searchSimilar(
                    expandedQuery, candidateLimit, candidateLimit, bookIdFilter);
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
                        (existing, replacement) -> existing // 杜绝 ID 重复导致的异常
                ));

        return bookIds.stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private int resolveRecommendCandidateLimit(int limit) {
        int safeLimit = Math.max(limit, 1);
        int multiplier = Math.max(recommendProperties.getCandidateMultiplier(), 1);
        return Math.max(recommendProperties.getMinCandidates(), safeLimit * multiplier);
    }

    private DeadLetterTarget resolveDeadLetterTarget(String taskType) {
        String normalizedTaskType = StringUtils.lowerCase(StringUtils.trimToEmpty(taskType), Locale.ROOT);
        return switch (normalizedTaskType) {
            case "summary" -> new DeadLetterTarget(summaryQueue + ".dlq", summaryRoutingKey);
            case "embedding" -> new DeadLetterTarget(embeddingQueue + ".dlq", embeddingRoutingKey);
            default -> throw new ServiceException("不支持的 RAG 死信任务类型: " + taskType);
        };
    }

    private int resolveDeadLetterRequeueLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_DLQ_REQUEUE_LIMIT;
        }
        return Math.min(limit, MAX_DLQ_REQUEUE_LIMIT);
    }

    private record DeadLetterTarget(String queueName, String routingKey) {
    }
}
