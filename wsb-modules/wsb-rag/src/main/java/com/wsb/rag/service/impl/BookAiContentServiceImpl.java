package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.rag.service.BookAiContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * AI 摘要服务实现，使用 DeepSeek Chat 生成摘要与聚合书评。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookAiContentServiceImpl implements BookAiContentService {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern MARKDOWN_EMPHASIS = Pattern.compile("(\\*\\*|__|\\*|`)");
    private static final String REVIEW_CACHE_KEY_PREFIX = "rag:review:book:";

    @Value("${deepseek.chat-url}")
    private String chatUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    @Value("${tavily.review-cache-days:7}")
    private long reviewCacheDays;

    @Value("${deepseek.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds;

    @Value("${deepseek.read-timeout-seconds:60}")
    private long readTimeoutSeconds;

    @Value("${deepseek.write-timeout-seconds:30}")
    private long writeTimeoutSeconds;

    @Value("${deepseek.call-timeout-seconds:90}")
    private long callTimeoutSeconds;

    private final RemoteBookService remoteBookService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile OkHttpClient httpClient;

    @Override
    public String getCachedReviewDigest(Long bookId) {
        return getCachedReview(bookId);
    }

    @Override
    public String generateSummary(Long bookId) {
        Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
        BookRemoteDTO book = result.getData();
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        String prompt = buildSummaryPrompt(book);
        String summary = sanitizeGeneratedText(callChatAPI(prompt));
        if (StringUtils.isBlank(summary)) {
            throw new ServiceException("AI 生成的摘要为空");
        }

        remoteBookService.updateSummary(bookId, summary);
        log.info("生成摘要成功: bookId={}", bookId);
        return summary;
    }

    @Override
    public String aggregateReviews(Long bookId, String title, String author) {
        log.info("开始聚合网络书评: bookId={}, title={}", bookId, title);

        String cachedReviews = getCachedReview(bookId);
        if (StringUtils.isNotBlank(cachedReviews)) {
            log.info("命中 Redis 网络书评缓存: bookId={}", bookId);
            return cachedReviews;
        }

        String searchQuery = String.format("《%s》%s 书评 读后感", title, StringUtils.defaultString(author));
        log.info("开始搜索网络书评: bookId={}, query={}", bookId, searchQuery);
        String reviews = searchWithTavily(searchQuery);
        log.info("Tavily 搜索结果: bookId={}\n{}", bookId, reviews);
        String prompt = buildReviewAggregationPrompt(title, author, reviews);
        String reviewResponse = callChatAPI(prompt);
        log.info("DeepSeek 返回书评结果: bookId={}\n{}", bookId, reviewResponse);
        String reviewDigest = sanitizeGeneratedText(reviewResponse);
        if (StringUtils.isBlank(reviewDigest)) {
            throw new ServiceException("聚合书评为空");
        }

        cacheReview(bookId, reviewDigest);
        log.info("聚合网络书评完成: bookId={}", bookId);
        return reviewDigest;
    }

    private String buildSummaryPrompt(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下书籍生成一段适合语义检索的中文摘要，控制在200到300字之间。\n");
        sb.append("这个摘要将参与图书 RAG 检索，请优先保留有助于召回的关键信息：题材类型、主题、时代背景或知识领域、核心人物或核心问题、主要内容、写作风格、阅读价值、适合读者。\n");
        sb.append("如果是文学作品，优先概括时代背景、核心人物、主要冲突和主题；如果是非虚构或知识类书籍，优先概括研究领域、核心议题、主要观点和方法。\n");
        sb.append("只输出摘要正文，不要标题，不要 Markdown，不要分点，不要项目符号，不要套话，不要编造书中没有的信息；尽量保留书名、作者名、专有名词和主题词。\n\n");
        sb.append("书名：").append(book.getTitle()).append("\n");
        if (StringUtils.isNotBlank(book.getSubtitle())) {
            sb.append("副标题：").append(book.getSubtitle()).append("\n");
        }
        if (StringUtils.isNotBlank(book.getAuthor())) {
            sb.append("作者：").append(book.getAuthor()).append("\n");
        }
        if (StringUtils.isNotBlank(book.getKeyword())) {
            sb.append("关键词：").append(book.getKeyword()).append("\n");
        }
        return sb.toString();
    }

    private String buildReviewAggregationPrompt(String title, String author, String reviews) {
        return String.format("""
                请根据以下网络搜索结果，为%s的《%s》整理 3 条左右简短的中文书评。
                要求：
                1. 每条书评控制在 50 到 100 字之间，尽量简短。
                2. 每条都要带来源平台。
                3. 每条只聚焦一个明确观点，可以写优点、缺点、阅读感受或适合人群。
                4. 按要求输出书评内容，要编号，不要总标题，不要 Markdown，不要编造搜索结果里没有的来源。
                5. 每条书评之间要换行。

                搜索结果：
                %s
                
                返回格式：
                1.书评内容（来源：xx）
                2.书评内容（来源：xx）
                ...
                
                """, StringUtils.defaultIfBlank(author, "未知"), title, reviews);
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
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = getHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Chat API 调用失败: code={}, body={}", response.code(), errorBody);
                    throw new ServiceException("AI 服务暂时不可用");
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("choices").path(0).path("message").path("content").asText();
            }
        } catch (SocketTimeoutException e) {
            log.error("Chat API 调用超时: connect={}s, read={}s, write={}s, call={}s",
                    connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds, callTimeoutSeconds, e);
            throw new ServiceException("AI 服务调用超时，请稍后重试");
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
                    .put("max_results", 5)
                    .put("include_answer", true));

            Request request = new Request.Builder()
                    .url("https://api.tavily.com/search")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = getHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Tavily API 调用失败: {}", response.code());
                    return "暂无网络书评信息";
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (JsonNode result : root.path("results")) {
                    sb.append(result.path("content").asText()).append("\n\n");
                    count++;
                }
                log.info("Tavily 搜索完成: query={}, results={}", query, count);
                return sb.toString();
            }
        } catch (IOException e) {
            log.error("Tavily 搜索异常", e);
            return "暂无网络书评信息";
        }
    }

    private String sanitizeGeneratedText(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }

        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        normalized = MARKDOWN_HEADING.matcher(normalized).replaceAll("");
        normalized = MARKDOWN_EMPHASIS.matcher(normalized).replaceAll("");
        normalized = normalized.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private String getCachedReview(Long bookId) {
        try {
            return stringRedisTemplate.opsForValue().get(REVIEW_CACHE_KEY_PREFIX + bookId);
        } catch (Exception e) {
            log.warn("读取网络书评 Redis 缓存失败: bookId={}", bookId, e);
            return null;
        }
    }

    private void cacheReview(Long bookId, String reviewDigest) {
        if (StringUtils.isBlank(reviewDigest)) {
            return;
        }
        try {
            long ttlDays = Math.max(reviewCacheDays, 1);
            stringRedisTemplate.opsForValue().set(
                    REVIEW_CACHE_KEY_PREFIX + bookId,
                    reviewDigest,
                    Duration.ofDays(ttlDays)
            );
            log.info("写入网络书评 Redis 缓存成功: bookId={}, ttlDays={}", bookId, ttlDays);
        } catch (Exception e) {
            log.warn("写入网络书评 Redis 缓存失败: bookId={}", bookId, e);
        }
    }

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                            .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return httpClient;
    }
}
