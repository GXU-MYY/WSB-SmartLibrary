package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量数据库服务实现（Supabase）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient();

    private static final String TABLE_NAME = "book_embeddings";

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
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("存储向量失败: {}", response.code());
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
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("向量搜索失败: {}", response.code());
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
            // 先获取该书籍的向量
            Request getRequest = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?book_id=eq." + bookId + "&select=embedding")
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("Authorization", "Bearer " + serviceRoleKey)
                    .get()
                    .build();

            try (Response getResponse = httpClient.newCall(getRequest).execute()) {
                if (!getResponse.isSuccessful()) {
                    log.error("获取书籍向量失败: {}", getResponse.code());
                    return List.of();
                }

                String responseBody = getResponse.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.isEmpty()) {
                    return List.of();
                }

                // 解析向量
                List<Float> embedding = new ArrayList<>();
                for (JsonNode value : root.get(0).path("embedding")) {
                    embedding.add((float) value.asDouble());
                }

                // 搜索相似书籍
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
                    log.error("删除向量失败: {}", response.code());
                } else {
                    log.info("删除向量成功: bookId={}", bookId);
                }
            }
        } catch (IOException e) {
            log.error("删除向量异常", e);
        }
    }
}
