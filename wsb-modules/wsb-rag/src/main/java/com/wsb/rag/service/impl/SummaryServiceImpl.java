package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * AI 摘要服务实现（DeepSeek Chat）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    @Value("${deepseek.chat-url}")
    private String chatUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    private final RemoteBookService remoteBookService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String generateSummary(Long bookId) {
        Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
        BookRemoteDTO book = result.getData();
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        // 构建提示词
        String prompt = buildSummaryPrompt(book);

        // 调用 DeepSeek Chat API
        String summary = callChatAPI(prompt);

        // 更新书籍摘要
        remoteBookService.updateSummary(bookId, summary);

        log.info("生成摘要成功: bookId={}", bookId);
        return summary;
    }

    @Override
    public String aggregateReviews(Long bookId, String title, String author) {
        // 搜索网络书评
        String searchQuery = String.format("《%s》 %s 书评 读后感", title, author != null ? author : "");
        String reviews = searchWithTavily(searchQuery);

        // 使用 AI 整合书评
        String prompt = buildReviewAggregationPrompt(title, author, reviews);
        return callChatAPI(prompt);
    }

    private String buildSummaryPrompt(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下书籍生成一段简洁的摘要（200-300字），包含主要内容、主题和价值：\n\n");
        sb.append("书名：").append(book.getTitle()).append("\n");
        if (book.getAuthor() != null) {
            sb.append("作者：").append(book.getAuthor()).append("\n");
        }
        if (book.getKeyword() != null) {
            sb.append("关键词：").append(book.getKeyword()).append("\n");
        }
        if (book.getLabel() != null) {
            sb.append("标签：").append(book.getLabel()).append("\n");
        }
        return sb.toString();
    }

    private String buildReviewAggregationPrompt(String title, String author, String reviews) {
        return String.format("""
                请根据以下网络搜索到的书评信息，为《%s》整理一份综合书评（300-500字）：
                要求：
                1. 总结读者的主要观点
                2. 提及书籍的优缺点
                3. 给出整体评价

                搜索结果：
                %s
                """, title, reviews);
    }

    private String callChatAPI(String prompt) {
        try {
            String json = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("model", "deepseek-chat")
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt))));

            Request request = new Request.Builder()
                    .url(chatUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Chat API 调用失败: {}", response.code());
                    throw new ServiceException("AI 服务暂时不可用");
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("choices").get(0).path("message").path("content").asText();
            }
        } catch (IOException e) {
            log.error("调用 Chat API 异常", e);
            throw new ServiceException("AI 服务调用失败: " + e.getMessage());
        }
    }

    private String searchWithTavily(String query) {
        try {
            String json = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("api_key", tavilyApiKey)
                    .put("query", query)
                    .put("search_depth", "basic")
                    .put("include_answer", true));

            Request request = new Request.Builder()
                    .url("https://api.tavily.com/search")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Tavily API 调用失败: {}", response.code());
                    return "暂无网络书评信息";
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                StringBuilder sb = new StringBuilder();
                for (JsonNode result : root.path("results")) {
                    sb.append(result.path("content").asText()).append("\n\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            log.error("Tavily 搜索异常", e);
            return "暂无网络书评信息";
        }
    }
}