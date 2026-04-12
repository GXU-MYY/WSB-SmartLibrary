package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量数据库服务实现（Supabase）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final String TABLE_NAME = "book_embeddings";

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void storeEmbedding(Long bookId, List<Float> embedding, BookRemoteDTO metadata) {
        try {
            String embeddingStr = embedding.toString();
            String json = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("book_id", bookId)
                    .put("title", metadata.getTitle())
                    .put("author", metadata.getAuthor())
                    .put("embedding", "[" + embeddingStr.substring(1, embeddingStr.length() - 1) + "]"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + TABLE_NAME)
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("Authorization", "Bearer " + serviceRoleKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("存储向量失败: {}, body={}", response.code(), errorBody);
                    throw new ServiceException("存储向量失败");
                }
                log.info("存储向量成功: bookId={}", bookId);
            }
        } catch (IOException e) {
            log.error("存储向量异常", e);
            throw new ServiceException("存储向量失败: " + e.getMessage());
        }
    }

    @Override
    public List<Long> searchSimilar(List<Float> queryEmbedding, int limit) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("跳过向量搜索：查询向量为空");
            return List.of();
        }

        try {
            String embeddingJson = queryEmbedding.toString();
            String rpcUrl = supabaseUrl + "/rest/v1/rpc/match_book_embeddings";

            String json = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("query_embedding", "[" + embeddingJson.substring(1, embeddingJson.length() - 1) + "]")
                    .put("match_count", limit));

            Request request = new Request.Builder()
                    .url(rpcUrl)
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("Authorization", "Bearer " + serviceRoleKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("向量搜索失败: {}, body={}", response.code(), errorBody);
                    return List.of();
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                List<Long> result = new ArrayList<>();
                for (JsonNode item : root) {
                    result.add(item.path("book_id").asLong());
                }
                return result;
            }
        } catch (IOException e) {
            log.error("向量搜索异常", e);
            return List.of();
        }
    }

    @Override
    public List<Long> getSimilarBooks(Long bookId, int limit) {
        try {
            Request getRequest = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?book_id=eq." + bookId + "&select=embedding")
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("Authorization", "Bearer " + serviceRoleKey)
                    .get()
                    .build();

            try (Response getResponse = httpClient.newCall(getRequest).execute()) {
                if (!getResponse.isSuccessful()) {
                    String errorBody = getResponse.body() != null ? getResponse.body().string() : "";
                    log.error("获取书籍向量失败: {}, body={}", getResponse.code(), errorBody);
                    return List.of();
                }

                String responseBody = getResponse.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.isEmpty()) {
                    return List.of();
                }

                List<Float> embedding = parseEmbedding(root.get(0).path("embedding"));
                if (embedding.isEmpty()) {
                    log.warn("书籍向量为空或解析失败: bookId={}", bookId);
                    return List.of();
                }

                return searchSimilar(embedding, limit + 1).stream()
                        .filter(id -> !id.equals(bookId))
                        .limit(limit)
                        .toList();
            }
        } catch (IOException e) {
            log.error("获取相似书籍异常", e);
            return List.of();
        }
    }

    @Override
    public void deleteEmbedding(Long bookId) {
        try {
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?book_id=eq." + bookId)
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("Authorization", "Bearer " + serviceRoleKey)
                    .delete()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("删除向量失败: {}, body={}", response.code(), errorBody);
                } else {
                    log.info("删除向量成功: bookId={}", bookId);
                }
            }
        } catch (IOException e) {
            log.error("删除向量异常", e);
        }
    }

    private List<Float> parseEmbedding(JsonNode embeddingNode) {
        List<Float> embedding = new ArrayList<>();
        if (embeddingNode == null || embeddingNode.isMissingNode() || embeddingNode.isNull()) {
            return embedding;
        }

        if (embeddingNode.isArray()) {
            for (JsonNode value : embeddingNode) {
                embedding.add((float) value.asDouble());
            }
            return embedding;
        }

        if (!embeddingNode.isTextual()) {
            return embedding;
        }

        String raw = embeddingNode.asText();
        if (StringUtils.isBlank(raw)) {
            return embedding;
        }

        String normalized = raw.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (StringUtils.isBlank(normalized)) {
            return embedding;
        }

        for (String item : normalized.split(",")) {
            String token = item.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                embedding.add(Float.parseFloat(token));
            } catch (NumberFormatException ex) {
                log.warn("书籍向量包含非法数字: token={}", token);
                return List.of();
            }
        }

        return embedding;
    }
}
