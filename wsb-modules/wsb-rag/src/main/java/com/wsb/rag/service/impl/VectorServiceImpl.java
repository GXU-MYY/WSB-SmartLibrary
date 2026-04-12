package com.wsb.rag.service.impl;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.config.RagPgVectorProperties;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 向量数据库服务实现（pgvector）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final PgVectorStore pgVectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final RagPgVectorProperties properties;

    @Override
    public void storeEmbedding(Long bookId, String content, BookRemoteDTO metadata) {
        if (bookId == null) {
            throw new ServiceException("书籍ID不能为空");
        }
        if (StringUtils.isBlank(content)) {
            throw new ServiceException("向量内容不能为空");
        }

        deleteEmbedding(bookId);

        Document document = new Document(content, buildMetadata(bookId, metadata));
        pgVectorStore.add(List.of(document));
        log.info("已写入 pgvector: bookId={}", bookId);
    }

    @Override
    public List<Long> searchSimilar(String query, int limit) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }

        return pgVectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(limit)
                        .build())
                .stream()
                .map(this::extractBookId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getSimilarBooks(Long bookId, int limit) {
        if (bookId == null) {
            return List.of();
        }

        String content = findContentByBookId(bookId);
        if (StringUtils.isBlank(content)) {
            log.warn("未找到可用于相似推荐的向量内容: bookId={}", bookId);
            return List.of();
        }

        return pgVectorStore.similaritySearch(SearchRequest.builder()
                        .query(content)
                        .topK(limit + 1)
                        .build())
                .stream()
                .map(this::extractBookId)
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(bookId))
                .distinct()
                .limit(limit)
                .toList();
    }

    @Override
    public void deleteEmbedding(Long bookId) {
        if (bookId == null) {
            return;
        }

        int deleted = jdbcTemplate.update(
                "DELETE FROM " + qualifiedTableName() + " WHERE metadata->>'bookId' = ?",
                bookId.toString()
        );
        if (deleted > 0) {
            log.info("已删除 pgvector 向量: bookId={}, rows={}", bookId, deleted);
        }
    }

    private Map<String, Object> buildMetadata(Long bookId, BookRemoteDTO metadata) {
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("bookId", bookId);
        if (metadata == null) {
            return metadataMap;
        }
        if (StringUtils.isNotBlank(metadata.getTitle())) {
            metadataMap.put("title", metadata.getTitle());
        }
        if (StringUtils.isNotBlank(metadata.getAuthor())) {
            metadataMap.put("author", metadata.getAuthor());
        }
        if (StringUtils.isNotBlank(metadata.getCoverUrl())) {
            metadataMap.put("coverUrl", metadata.getCoverUrl());
        }
        return metadataMap;
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
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String findContentByBookId(Long bookId) {
        List<String> contents = jdbcTemplate.query(
                "SELECT content FROM " + qualifiedTableName() + " WHERE metadata->>'bookId' = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("content"),
                bookId.toString()
        );
        return contents.isEmpty() ? null : contents.get(0);
    }

    private String qualifiedTableName() {
        return safeIdentifier(properties.getSchemaName()) + "." + safeIdentifier(properties.getTableName());
    }

    private String safeIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new ServiceException("非法的 pgvector 标识符配置: " + identifier);
        }
        return identifier;
    }
}
