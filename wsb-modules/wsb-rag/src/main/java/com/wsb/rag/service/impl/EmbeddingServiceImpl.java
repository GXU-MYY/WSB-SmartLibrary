package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedding 服务实现（DeepSeek）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${deepseek.embedding-url}")
    private String embeddingUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public List<Float> generateEmbedding(String text) {
        List<List<Float>> embeddings = generateEmbeddings(List.of(text));
        if (embeddings.isEmpty()) {
            throw new ServiceException("生成嵌入向量失败");
        }
        return embeddings.get(0);
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        try {
            String requestBody = objectMapper.writeValueAsString(objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode()
                            .put("model", "BAAI/bge-large-zh-v1.5")
                            .set("input", objectMapper.valueToTree(texts))));

            Request request = new Request.Builder()
                    .url(embeddingUrl + "/v1/embeddings")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Embedding API 调用失败: {}", response.code());
                    throw new ServiceException("Embedding API 调用失败");
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.path("data");

                List<List<Float>> result = new ArrayList<>();
                for (JsonNode item : data) {
                    List<Float> embedding = new ArrayList<>();
                    for (JsonNode value : item.path("embedding")) {
                        embedding.add((float) value.asDouble());
                    }
                    result.add(embedding);
                }
                return result;
            }
        } catch (IOException e) {
            log.error("生成嵌入向量异常", e);
            throw new ServiceException("生成嵌入向量失败: " + e.getMessage());
        }
    }
}