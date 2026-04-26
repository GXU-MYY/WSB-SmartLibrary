package com.wsb.rag.service.impl;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.config.RagPgVectorProperties;
import com.wsb.rag.mapper.BookEmbeddingMapper;
import com.wsb.rag.mapper.BookRankRow;
import com.wsb.rag.service.VectorService;
import com.wsb.rag.util.ClcCategoryUtils;
import com.wsb.rag.util.QueryTextAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private static final String CHUNK_IDENTITY = "identity";
    private static final String CHUNK_SUBJECT = "subject";
    private static final String CHUNK_SUMMARY = "summary";
    private static final int MAX_QUERY_PATTERNS = 4;
    private static final double CHUNK_WEIGHT_BOOST_STEP = 0.10;

    private final PgVectorStore pgVectorStore;
    private final BookEmbeddingMapper bookEmbeddingMapper;
    private final RagPgVectorProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeEmbedding(Long bookId, BookRemoteDTO metadata) {
        if (bookId == null) {
            throw new ServiceException("book id must not be null");
        }

        List<Document> documents = buildDocuments(bookId, metadata);
        if (documents.isEmpty()) {
            throw new ServiceException("embedding content must not be empty");
        }

        deleteEmbedding(bookId);
        pgVectorStore.add(documents);
        log.info("stored pgvector embeddings: bookId={}, chunks={}", bookId, documents.size());
    }

    @Override
    public List<Long> searchSimilar(String query, int limit) {
        return searchSimilar(query, limit, Set.of());
    }

    @Override
    public List<Long> searchSimilar(String query, int limit, Set<Long> bookIdFilter) {
        return searchSimilar(query, limit, resolveCandidateLimit(limit), bookIdFilter);
    }

    @Override
    public List<Long> searchSimilar(String query, int limit, int candidateLimit, Set<Long> bookIdFilter) {
        if (StringUtils.isBlank(query) || limit <= 0) {
            return List.of();
        }

        int safeCandidateLimit = Math.max(candidateLimit, limit);
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Integer> bestRanks = new HashMap<>();
        List<BookRank> vectorRanks = safeSearchVectorBookRanks(query, safeCandidateLimit, bookIdFilter);
        List<BookRank> keywordRanks = searchKeywordBookRanks(query, safeCandidateLimit, bookIdFilter);

        mergeRrfScores(scores, bestRanks, vectorRanks, properties.getVectorScoreWeight());
        mergeRrfScores(scores, bestRanks, keywordRanks, properties.getKeywordScoreWeight());

        log.info(
                "hybrid recall completed: query={}, vectorHits={}, keywordHits={}, mergedHits={}, ownerFilterSize={}",
                query,
                vectorRanks.size(),
                keywordRanks.size(),
                scores.size(),
                bookIdFilter == null ? 0 : bookIdFilter.size()
        );

        return scores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> bestRanks.getOrDefault(entry.getKey(), Integer.MAX_VALUE))
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public List<Long> getSimilarBooks(Long bookId, int limit) {
        if (bookId == null || limit <= 0) {
            return List.of();
        }

        try {
            return bookEmbeddingMapper.searchSimilarBookRanks(
                            qualifiedTableName(),
                            bookId,
                            resolveCandidateLimit(limit + 1),
                            limit
                    )
                    .stream()
                    .map(BookRankRow::getBookId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("similar book recall failed: bookId={}", bookId, e);
            return List.of();
        }
    }

    @Override
    public void deleteEmbedding(Long bookId) {
        if (bookId == null) {
            return;
        }

        int deleted = bookEmbeddingMapper.deleteByBookId(qualifiedTableName(), bookId.toString());
        if (deleted > 0) {
            log.info("deleted pgvector embeddings: bookId={}, rows={}", bookId, deleted);
        }
    }

    private List<Document> buildDocuments(Long bookId, BookRemoteDTO book) {
        List<Document> documents = new ArrayList<>();
        if (book == null) {
            return documents;
        }

        addDocument(documents, bookId, book, CHUNK_IDENTITY, 4, buildIdentityText(book));
        addDocument(documents, bookId, book, CHUNK_SUBJECT, 3, buildSubjectText(book));
        addDocument(documents, bookId, book, CHUNK_SUMMARY, 1, buildSummaryText(book));
        return documents;
    }

    private void addDocument(List<Document> documents, Long bookId, BookRemoteDTO book,
                             String chunkType, int chunkWeight, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        documents.add(new Document(text, buildMetadata(bookId, book, chunkType, chunkWeight)));
    }

    private String buildIdentityText(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "书名", book.getTitle());
        appendField(sb, "作者", book.getAuthor());
        return sb.toString();
    }

    private String buildSubjectText(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "书名", book.getTitle());
        appendField(sb, "关键词", book.getKeyword());
        appendField(sb, "中图分类", ClcCategoryUtils.resolveCategory(book.getClc()));
        appendField(sb, "中图分类号", book.getClc());
        return sb.toString();
    }

    private String buildSummaryText(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "书名", book.getTitle());
        appendField(sb, "摘要", book.getSummary());
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String fieldName, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(" | ");
        }
        sb.append(fieldName).append(": ").append(value.trim());
    }

    private Map<String, Object> buildMetadata(Long bookId, BookRemoteDTO metadata, String chunkType, int chunkWeight) {
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("bookId", bookId);
        metadataMap.put("chunkType", chunkType);
        metadataMap.put("chunkWeight", chunkWeight);
        if (metadata == null) {
            return metadataMap;
        }
        putIfNotBlank(metadataMap, "title", metadata.getTitle());
        putIfNotBlank(metadataMap, "author", metadata.getAuthor());
        putIfNotBlank(metadataMap, "keyword", metadata.getKeyword());
        putIfNotBlank(metadataMap, "clc", metadata.getClc());
        putIfNotBlank(metadataMap, "clcCategory", ClcCategoryUtils.resolveCategory(metadata.getClc()));
        putIfNotBlank(metadataMap, "coverUrl", metadata.getCoverUrl());
        return metadataMap;
    }

    private void putIfNotBlank(Map<String, Object> metadataMap, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            metadataMap.put(key, value);
        }
    }

    private List<BookRank> searchVectorBookRanks(String query, int candidateLimit, Set<Long> bookIdFilter) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(query)
                .topK(candidateLimit)
                .similarityThreshold(properties.getSimilarityThreshold());
        Filter.Expression filterExpression = buildBookIdFilterExpression(bookIdFilter);
        if (filterExpression != null) {
            searchRequestBuilder.filterExpression(filterExpression);
        }

        List<Document> documents = pgVectorStore.similaritySearch(searchRequestBuilder.build());

        Map<Long, Double> rankedBooks = new HashMap<>();
        for (Document document : documents) {
            Long bookId = extractBookId(document);
            if (bookId == null) {
                continue;
            }
            rankedBooks.merge(bookId, resolveVectorBoost(document), Math::max);
        }

        return rankedBooks.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new BookRank(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<BookRank> safeSearchVectorBookRanks(String query, int candidateLimit, Set<Long> bookIdFilter) {
        try {
            return searchVectorBookRanks(query, candidateLimit, bookIdFilter);
        } catch (RuntimeException e) {
            log.warn("vector recall failed, fallback to keyword recall only: query={}", query, e);
            return List.of();
        }
    }

    private List<BookRank> searchKeywordBookRanks(String query, int candidateLimit, Set<Long> bookIdFilter) {
        try {
            return bookEmbeddingMapper.searchKeywordBookRanks(
                            qualifiedTableName(),
                            query,
                            toLikePattern(query),
                            buildQueryPatterns(query),
                            bookIdFilter.stream().sorted().toList(),
                            candidateLimit
                    )
                    .stream()
                    .map(row -> new BookRank(row.getBookId(), resolveRankScore(row)))
                    .filter(rank -> rank.bookId() != null)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("keyword recall failed, fallback to vector recall only: query={}", query, e);
            return List.of();
        }
    }

    private Filter.Expression buildBookIdFilterExpression(Set<Long> bookIdFilter) {
        if (bookIdFilter == null || bookIdFilter.isEmpty()) {
            return null;
        }
        List<Object> values = bookIdFilter.stream()
                .sorted()
                .map(id -> (Object) id)
                .toList();
        return new FilterExpressionBuilder().in("bookId", values).build();
    }

    private List<String> buildQueryPatterns(String query) {
        LinkedHashMap<String, Boolean> terms = new LinkedHashMap<>();
        for (String term : QueryTextAnalyzer.extractTerms(query, MAX_QUERY_PATTERNS)) {
            addQueryTerm(terms, term);
        }
        if (terms.isEmpty()) {
            addQueryTerm(terms, query);
        }
        return terms.keySet().stream()
                .limit(MAX_QUERY_PATTERNS)
                .map(this::toLikePattern)
                .toList();
    }

    private void addQueryTerm(Map<String, Boolean> terms, String term) {
        String normalized = QueryTextAnalyzer.normalize(term);
        if (StringUtils.isNotBlank(normalized)) {
            terms.putIfAbsent(normalized, Boolean.TRUE);
        }
    }

    private String toLikePattern(String value) {
        return "%" + escapeLike(QueryTextAnalyzer.normalize(value)) + "%";
    }

    private String escapeLike(String value) {
        return StringUtils.defaultString(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private void mergeRrfScores(Map<Long, Double> scores, Map<Long, Integer> bestRanks,
                                List<BookRank> ranks, double sourceWeight) {
        int rank = 1;
        for (BookRank item : ranks) {
            if (item.bookId() == null) {
                continue;
            }
            double boost = Math.max(1.0, Math.min(item.boost(), 4.0));
            double score = sourceWeight * boost / (properties.getRrfRankConstant() + rank);
            scores.merge(item.bookId(), score, Double::sum);
            bestRanks.merge(item.bookId(), rank, Math::min);
            rank++;
        }
    }

    private int resolveCandidateLimit(int limit) {
        int safeLimit = Math.max(limit, 1);
        int multiplier = Math.max(properties.getHybridCandidateMultiplier(), 1);
        return Math.max(properties.getHybridMinCandidates(), safeLimit * multiplier);
    }

    private Long extractBookId(Document document) {
        if (document == null || document.getMetadata() == null) {
            return null;
        }
        Object value = document.getMetadata().get("bookId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return parseBookId(text);
        }
        return null;
    }

    private double extractChunkWeight(Document document) {
        if (document == null || document.getMetadata() == null) {
            return 1.0;
        }
        Object value = document.getMetadata().get("chunkWeight");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 1.0;
            }
        }
        return 1.0;
    }

    private double extractSimilarityScore(Document document) {
        if (document == null || document.getScore() == null) {
            return 1.0;
        }
        return Math.max(document.getScore(), 0.0);
    }

    private double resolveVectorBoost(Document document) {
        double similarityScore = extractSimilarityScore(document);
        double chunkWeight = Math.max(extractChunkWeight(document), 1.0);
        return similarityScore * (1.0 + (chunkWeight - 1.0) * CHUNK_WEIGHT_BOOST_STEP);
    }

    private Long parseBookId(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double resolveRankScore(BookRankRow row) {
        if (row == null || row.getRankScore() == null) {
            return 1.0;
        }
        return row.getRankScore();
    }

    private String qualifiedTableName() {
        return safeIdentifier(properties.getSchemaName()) + "." + safeIdentifier(properties.getTableName());
    }

    private String safeIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new ServiceException("invalid pgvector identifier: " + identifier);
        }
        return identifier;
    }

    private record BookRank(Long bookId, double boost) {
    }
}
