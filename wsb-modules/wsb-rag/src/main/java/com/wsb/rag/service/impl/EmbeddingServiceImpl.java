package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.EmbeddingService;
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
 * Embedding 服务实现（DashScope Qwen Embedding）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final MediaType JSON = MediaType.parse("application/json");

    @Value("${dashscope.embedding-base-url}")
    private String embeddingBaseUrl;

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.embedding-model}")
    private String embeddingModel;

    @Value("${dashscope.embedding-dimensions}")
    private Integer embeddingDimensions;

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
        validateConfig();

        try {
            var requestPayload = objectMapper.createObjectNode();
            requestPayload.put("model", embeddingModel);
            requestPayload.set("input", objectMapper.valueToTree(texts));
            requestPayload.put("encoding_format", "float");
            requestPayload.put("dimensions", embeddingDimensions);
            String requestBody = objectMapper.writeValueAsString(requestPayload);

            Request request = new Request.Builder()
                    .url(resolveEmbeddingEndpoint())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("DashScope Embedding API 调用失败: code={}, body={}", response.code(), errorBody);
                    throw new ServiceException("DashScope Embedding API 调用失败");
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

    private void validateConfig() {
        if (StringUtils.isBlank(embeddingBaseUrl)) {
            throw new ServiceException("未配置 DashScope Embedding 服务地址");
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new ServiceException("未配置 DashScope API Key");
        }
        if (StringUtils.isBlank(embeddingModel)) {
            throw new ServiceException("未配置 DashScope Embedding 模型");
        }
    }

    private String resolveEmbeddingEndpoint() {
        return StringUtils.removeEnd(embeddingBaseUrl, "/") + "/embeddings";
    }
}
