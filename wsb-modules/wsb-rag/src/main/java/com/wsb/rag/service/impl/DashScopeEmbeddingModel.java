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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DashScope Embedding 模型适配器。
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

    @Value("${dashscope.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds;

    @Value("${dashscope.read-timeout-seconds:60}")
    private long readTimeoutSeconds;

    @Value("${dashscope.write-timeout-seconds:30}")
    private long writeTimeoutSeconds;

    @Value("${dashscope.call-timeout-seconds:90}")
    private long callTimeoutSeconds;

    @Value("${dashscope.embedding-batch-size:25}")
    private int embeddingBatchSize;

    @Value("${dashscope.embedding-max-attempts:3}")
    private int embeddingMaxAttempts;

    @Value("${dashscope.embedding-retry-backoff-millis:500}")
    private long embeddingRetryBackoffMillis;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile OkHttpClient httpClient;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        validateConfig();
        List<String> texts = request.getInstructions();
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingResponse(List.of());
        }

        try {
            List<Embedding> embeddings = new ArrayList<>(texts.size());
            int batchSize = resolveEmbeddingBatchSize();
            for (int start = 0; start < texts.size(); start += batchSize) {
                int end = Math.min(start + batchSize, texts.size());
                embeddings.addAll(callEmbeddingApiWithRetry(texts.subList(start, end), start));
            }
            return new EmbeddingResponse(embeddings);
        } catch (SocketTimeoutException e) {
            log.error("DashScope Embedding API 调用超时: connect={}s, read={}s, write={}s, call={}s",
                    connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds, callTimeoutSeconds, e);
            throw new ServiceException("生成嵌入向量超时，请稍后重试");
        } catch (IOException e) {
            log.error("生成嵌入向量异常", e);
            throw new ServiceException("生成嵌入向量失败: " + e.getMessage());
        }
    }

    private List<Embedding> callEmbeddingApiWithRetry(List<String> texts, int startIndex) throws IOException {
        int maxAttempts = Math.max(embeddingMaxAttempts, 1);
        IOException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callEmbeddingApi(texts, startIndex);
            } catch (IOException e) {
                lastException = e;
                if (attempt >= maxAttempts) {
                    throw e;
                }
                log.warn("DashScope Embedding API 调用失败，准备重试: attempt={}/{}, batchSize={}",
                        attempt, maxAttempts, texts.size(), e);
                sleepBeforeRetry(attempt);
            }
        }
        throw lastException != null ? lastException : new IOException("DashScope Embedding API 调用失败");
    }

    private List<Embedding> callEmbeddingApi(List<String> texts, int startIndex) throws IOException {
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

        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("DashScope Embedding API 调用失败: code={}, body={}", response.code(), errorBody);
                if (isRetryableStatus(response.code())) {
                    throw new IOException("DashScope Embedding API 可重试失败: code=" + response.code());
                }
                throw new ServiceException("DashScope Embedding API 调用失败");
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (JsonNode item : data) {
                float[] vector = toVector(item.path("embedding"));
                embeddings.add(new Embedding(vector, startIndex + index++));
            }
            return embeddings;
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

    private int resolveEmbeddingBatchSize() {
        return Math.max(1, Math.min(embeddingBatchSize, 25));
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt) throws IOException {
        long backoffMillis = Math.max(embeddingRetryBackoffMillis, 1);
        long sleepMillis = Math.min(backoffMillis * (1L << Math.min(attempt - 1, 4)), 10_000L);
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("DashScope Embedding API 重试被中断", e);
        }
    }

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(Math.max(connectTimeoutSeconds, 1), TimeUnit.SECONDS)
                            .readTimeout(Math.max(readTimeoutSeconds, 1), TimeUnit.SECONDS)
                            .writeTimeout(Math.max(writeTimeoutSeconds, 1), TimeUnit.SECONDS)
                            .callTimeout(Math.max(callTimeoutSeconds, 1), TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return httpClient;
    }
}
