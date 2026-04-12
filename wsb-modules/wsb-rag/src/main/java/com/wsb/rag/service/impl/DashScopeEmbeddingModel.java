package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DashScope-compatible embedding model adapter for Spring AI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeEmbeddingModel implements EmbeddingModel {

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
    public EmbeddingResponse call(EmbeddingRequest request) {
        validateConfig();
        List<String> texts = request.getInstructions();
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingResponse(List.of());
        }

        try {
            var requestPayload = objectMapper.createObjectNode();
            requestPayload.put("model", embeddingModel);
            requestPayload.set("input", objectMapper.valueToTree(texts));
            requestPayload.put("encoding_format", "float");
            requestPayload.put("dimensions", embeddingDimensions);

            Request httpRequest = new Request.Builder()
                    .url(resolveEmbeddingEndpoint())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(requestPayload), JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("DashScope Embedding API 调用失败: code={}, body={}", response.code(), errorBody);
                    throw new ServiceException("DashScope Embedding API 调用失败");
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.path("data");

                List<Embedding> embeddings = new ArrayList<>();
                int index = 0;
                for (JsonNode item : data) {
                    float[] vector = toVector(item.path("embedding"));
                    embeddings.add(new Embedding(vector, index++));
                }
                return new EmbeddingResponse(embeddings);
            }
        } catch (IOException e) {
            log.error("生成嵌入向量异常", e);
            throw new ServiceException("生成嵌入向量失败: " + e.getMessage());
        }
    }

    @Override
    public float[] embed(Document document) {
        if (document == null || StringUtils.isBlank(document.getText())) {
            return new float[0];
        }
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return embeddingDimensions != null ? embeddingDimensions : 1024;
    }

    private float[] toVector(JsonNode embeddingNode) {
        float[] vector = new float[embeddingNode.size()];
        int index = 0;
        for (JsonNode value : embeddingNode) {
            vector[index++] = (float) value.asDouble();
        }
        return vector;
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
