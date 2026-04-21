package com.wsb.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.config.RagRecommendProperties;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final int OWNED_RECOMMEND_MIN_CANDIDATES = 60;
    private static final int OWNED_RECOMMEND_MULTIPLIER = 6;
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Pattern QUERY_TERM_SPLITTER = Pattern.compile("[\\s,.;:|/\\\\]+");

    private final RemoteBookService remoteBookService;
    private final VectorService vectorService;
    private final RabbitTemplate rabbitTemplate;
    private final RagRecommendProperties recommendProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile OkHttpClient rerankHttpClient;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Value("${deepseek.chat-url:}")
    private String chatUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Override
    public List<BookRemoteDTO> recommend(String query, int limit, Long ownerId) {
        if (StringUtils.isBlank(query) || limit <= 0) {
            return List.of();
        }

        int candidateLimit = resolveRecommendCandidateLimit(limit);
        List<Long> bookIds = ownerId == null
                ? vectorService.searchSimilar(query, candidateLimit)
                : searchOwnedBooks(query, candidateLimit, ownerId);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        List<BookRemoteDTO> candidates = fetchBooksInOrder(bookIds);
        return rerankBooks(query, candidates, limit);
    }

    @Override
    public List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit) {
        List<Long> bookIds = vectorService.getSimilarBooks(bookId, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        return fetchBooksInOrder(bookIds);
    }

    @Override
    public void enqueueSummary(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, summaryRoutingKey, bookId);
        log.info("已发送摘要生成任务: bookId={}", bookId);
    }

    @Override
    public void processNewBook(Long bookId) {
        enqueueSummary(bookId);
    }

    public void enqueueEmbedding(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
        log.info("已发送向量生成任务: bookId={}", bookId);
    }

    private List<Long> searchOwnedBooks(String query, int limit, Long ownerId) {
        Result<List<Long>> ownedBookIdsResult = remoteBookService.getBookIdsByOwner(ownerId);
        List<Long> ownedBookIds = ownedBookIdsResult.getData();
        if (ownedBookIds == null || ownedBookIds.isEmpty()) {
            return List.of();
        }

        Set<Long> ownedBookIdSet = Set.copyOf(ownedBookIds);
        int candidateLimit = Math.max(OWNED_RECOMMEND_MIN_CANDIDATES, limit * OWNED_RECOMMEND_MULTIPLIER);

        return vectorService.searchSimilar(query, candidateLimit)
                .stream()
                .filter(ownedBookIdSet::contains)
                .limit(limit)
                .toList();
    }

    private List<BookRemoteDTO> fetchBooksInOrder(List<Long> bookIds) {
        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        List<BookRemoteDTO> books = result.getData();
        if (books == null || books.isEmpty()) {
            return List.of();
        }

        Map<Long, BookRemoteDTO> booksById = books.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        BookRemoteDTO::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return bookIds.stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<BookRemoteDTO> rerankBooks(String query, List<BookRemoteDTO> books, int limit) {
        if (books.isEmpty()) {
            return List.of();
        }

        List<BookRemoteDTO> localReranked = rerankBooksLocally(query, books);
        Map<Long, Double> llmScores = rerankTopCandidatesWithLlm(query, localReranked);
        if (llmScores.isEmpty()) {
            return localReranked.stream().limit(limit).toList();
        }

        Map<Long, Integer> localRanks = buildRankMap(localReranked);
        int rerankSize = Math.min(resolveLlmRerankMaxCandidates(), localReranked.size());
        List<BookRemoteDTO> topCandidates = localReranked.subList(0, rerankSize);
        Set<Long> rerankedIds = new HashSet<>();

        List<BookRemoteDTO> reranked = new ArrayList<>(topCandidates.stream()
                .filter(book -> book.getId() != null)
                .sorted(Comparator
                        .comparingDouble((BookRemoteDTO book) -> llmScores.getOrDefault(book.getId(), 0.0))
                        .reversed()
                        .thenComparing(book -> localRanks.getOrDefault(book.getId(), Integer.MAX_VALUE)))
                .peek(book -> rerankedIds.add(book.getId()))
                .toList());

        localReranked.stream()
                .filter(book -> book.getId() == null || !rerankedIds.contains(book.getId()))
                .forEach(reranked::add);

        return reranked.stream().limit(limit).toList();
    }

    private List<BookRemoteDTO> rerankBooksLocally(String query, List<BookRemoteDTO> books) {
        List<String> terms = extractQueryTerms(query);
        List<ScoredBook> scoredBooks = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            BookRemoteDTO book = books.get(i);
            scoredBooks.add(new ScoredBook(book, calculateLocalScore(query, terms, book, i), i));
        }

        return scoredBooks.stream()
                .sorted(Comparator
                        .comparingDouble(ScoredBook::score)
                        .reversed()
                        .thenComparingInt(ScoredBook::originalRank))
                .map(ScoredBook::book)
                .toList();
    }

    private double calculateLocalScore(String query, List<String> terms, BookRemoteDTO book, int originalRank) {
        double score = 1.0 / (originalRank + 1);
        score += calculateFieldScore(query, terms, book.getTitle(), 6.0);
        score += calculateFieldScore(query, terms, book.getAuthor(), 5.0);
        score += calculateFieldScore(query, terms, book.getKeyword(), 4.0);
        score += calculateFieldScore(query, terms, book.getSummary(), 1.0);
        return score;
    }

    private double calculateFieldScore(String query, List<String> terms, String fieldValue, double weight) {
        if (StringUtils.isBlank(fieldValue)) {
            return 0.0;
        }

        String normalizedField = fieldValue.toLowerCase(Locale.ROOT);
        String normalizedQuery = StringUtils.defaultString(query).trim().toLowerCase(Locale.ROOT);
        double score = 0.0;
        if (StringUtils.isNotBlank(normalizedQuery)) {
            if (normalizedField.equals(normalizedQuery)) {
                score += weight * 3.0;
            } else if (normalizedField.contains(normalizedQuery)) {
                score += weight * 2.0;
            }
        }

        for (String term : terms) {
            if (StringUtils.isNotBlank(term) && normalizedField.contains(term)) {
                score += weight;
            }
        }
        return score;
    }

    private Map<Long, Double> rerankTopCandidatesWithLlm(String query, List<BookRemoteDTO> localReranked) {
        if (!recommendProperties.isLlmRerankEnabled()
                || StringUtils.isBlank(chatUrl)
                || StringUtils.isBlank(apiKey)
                || localReranked.isEmpty()) {
            return Map.of();
        }

        int rerankSize = Math.min(resolveLlmRerankMaxCandidates(), localReranked.size());
        if (rerankSize <= 0) {
            return Map.of();
        }

        List<BookRemoteDTO> candidates = localReranked.subList(0, rerankSize);
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", StringUtils.defaultIfBlank(recommendProperties.getRerankModel(), "deepseek-chat"));
            payload.put("temperature", 0);

            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", "你负责对图书馆推荐结果重排序，只返回 JSON 数组。"));
            messages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", buildRerankPrompt(query, candidates)));
            payload.set("messages", messages);

            Request request = new Request.Builder()
                    .url(StringUtils.removeEnd(chatUrl, "/") + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                    .build();

            try (Response response = getRerankHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("LLM 重排序失败: code={}", response.code());
                    return Map.of();
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                String content = root.path("choices").path(0).path("message").path("content").asText();
                return parseRerankScores(content, candidates);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("LLM 重排序不可用，回退为本地重排序", e);
            return Map.of();
        }
    }

    private String buildRerankPrompt(String query, List<BookRemoteDTO> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询: ").append(query).append("\n");
        sb.append("请按推荐相关性给每个候选图书打 0 到 1 分。");
        sb.append("优先考虑书名、作者、专有名词、主题和读者意图的精确匹配。");
        sb.append("只返回 JSON，格式: [{\"bookId\":1,\"score\":0.95}].\n\n");
        sb.append("候选图书:\n");
        for (BookRemoteDTO book : candidates) {
            if (book.getId() == null) {
                continue;
            }
            sb.append("bookId=").append(book.getId())
                    .append("; 书名=").append(StringUtils.defaultString(book.getTitle()))
                    .append("; 作者=").append(StringUtils.defaultString(book.getAuthor()))
                    .append("; 关键词=").append(StringUtils.defaultString(book.getKeyword()))
                    .append("; 摘要=").append(StringUtils.abbreviate(StringUtils.defaultString(book.getSummary()), 240))
                    .append("\n");
        }
        return sb.toString();
    }

    private Map<Long, Double> parseRerankScores(String content, List<BookRemoteDTO> candidates) throws IOException {
        String json = extractJsonArray(content);
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }

        Set<Long> candidateIds = candidates.stream()
                .map(BookRemoteDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Double> scores = new HashMap<>();
        JsonNode array = objectMapper.readTree(json);
        if (!array.isArray()) {
            return Map.of();
        }

        for (JsonNode item : array) {
            long bookId = item.path("bookId").asLong(0);
            if (!candidateIds.contains(bookId)) {
                continue;
            }
            double score = item.path("score").asDouble(0.0);
            scores.put(bookId, Math.max(0.0, Math.min(score, 1.0)));
        }
        return scores;
    }

    private String extractJsonArray(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        return content.substring(start, end + 1);
    }

    private Map<Long, Integer> buildRankMap(List<BookRemoteDTO> books) {
        Map<Long, Integer> ranks = new HashMap<>();
        for (int i = 0; i < books.size(); i++) {
            Long bookId = books.get(i).getId();
            if (bookId != null) {
                ranks.putIfAbsent(bookId, i);
            }
        }
        return ranks;
    }

    private List<String> extractQueryTerms(String query) {
        String normalized = StringUtils.defaultString(query).trim().toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }

        List<String> terms = QUERY_TERM_SPLITTER.splitAsStream(normalized)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(12)
                .toList();
        return terms.isEmpty() ? List.of(normalized) : terms;
    }

    private int resolveRecommendCandidateLimit(int limit) {
        int safeLimit = Math.max(limit, 1);
        int multiplier = Math.max(recommendProperties.getCandidateMultiplier(), 1);
        return Math.max(recommendProperties.getMinCandidates(), safeLimit * multiplier);
    }

    private int resolveLlmRerankMaxCandidates() {
        return Math.max(recommendProperties.getLlmRerankMaxCandidates(), 0);
    }

    private OkHttpClient getRerankHttpClient() {
        if (rerankHttpClient == null) {
            synchronized (this) {
                if (rerankHttpClient == null) {
                    long timeoutSeconds = Math.max(recommendProperties.getRerankTimeoutSeconds(), 1);
                    rerankHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return rerankHttpClient;
    }

    private record ScoredBook(BookRemoteDTO book, double score, int originalRank) {
    }
}
