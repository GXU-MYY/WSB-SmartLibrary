package com.wsb.rag.consumer;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.constant.EmbeddingStatus;
import com.wsb.rag.service.BookAiContentService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagConsumer {

    private static final String SUMMARY_INFLIGHT_KEY_PREFIX = "rag:inflight:summary:";
    private static final String EMBEDDING_INFLIGHT_KEY_PREFIX = "rag:inflight:embedding:";
    private static final int RESULT_SUCCESS_CODE = 200;

    private final RemoteBookService remoteBookService;
    private final VectorService vectorService;
    private final BookAiContentService bookAiContentService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.inflight-ttl-minutes:30}")
    private long inflightTtlMinutes;

    @RabbitListener(queues = "${rag.queue.summary}", containerFactory = "ragRabbitListenerContainerFactory")
    public void processSummary(Long bookId) {
        log.info("开始生成摘要: bookId={}", bookId);
        runWithInflightLock(SUMMARY_INFLIGHT_KEY_PREFIX, bookId, () -> {
            String summary = bookAiContentService.generateSummary(bookId);
            if (StringUtils.isBlank(summary)) {
                throw new IllegalStateException("摘要生成结果为空: bookId=" + bookId);
            }
            rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
            log.info("摘要生成完成并已发送向量任务: bookId={}", bookId);
        });
    }

    @RabbitListener(queues = "${rag.queue.embedding}", containerFactory = "ragRabbitListenerContainerFactory")
    public void processEmbedding(Long bookId) {
        log.info("开始生成向量: bookId={}", bookId);
        runWithInflightLock(EMBEDDING_INFLIGHT_KEY_PREFIX, bookId, () -> {
            processEmbeddingWithStatus(bookId);
        });
    }

    private void processEmbeddingWithStatus(Long bookId) {
        boolean needResetPending = true;
        try {
            Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
            requireSuccess(result, "获取图书信息失败: bookId=" + bookId);
            BookRemoteDTO book = result.getData();
            if (book == null) {
                log.warn("图书不存在，跳过向量生成: bookId={}", bookId);
                needResetPending = false;
                return;
            }
            if (StringUtils.isBlank(book.getSummary())) {
                log.info("摘要未就绪，等待摘要生成: bookId={}", bookId);
                safeUpdateEmbeddingStatus(bookId, EmbeddingStatus.PENDING);
                needResetPending = false;
                return;
            }

            String canonicalKey = resolveCanonicalKey(book, bookId);
            if (vectorService.existsByCanonicalKey(canonicalKey)) {
                log.info("canonicalKey 向量已存在，跳过重复向量化: bookId={}, canonicalKey={}", bookId, canonicalKey);
                safeUpdateEmbeddingStatus(bookId, EmbeddingStatus.COMPLETED);
                needResetPending = false;
                return;
            }

            requireSuccess(remoteBookService.updateEmbeddingStatus(bookId, EmbeddingStatus.PROCESSING),
                "更新图书向量处理中状态失败: bookId=" + bookId);

            vectorService.storeEmbedding(bookId, book);
            safeUpdateEmbeddingStatus(bookId, EmbeddingStatus.COMPLETED);
            needResetPending = false;
            log.info("向量生成完成: bookId={}", bookId);
        } catch (RuntimeException e) {
            log.error("向量生成失败: bookId={}", bookId, e);
            throw e;
        } finally {
            if (needResetPending) {
                safeUpdateEmbeddingStatus(bookId, EmbeddingStatus.PENDING);
            }
        }
    }

    private String resolveCanonicalKey(BookRemoteDTO book, Long bookId) {
        if (StringUtils.isNotBlank(book.getIsbn())) {
            return "ISBN:" + book.getIsbn();
        }
        if (StringUtils.isNotBlank(book.getIsbn10())) {
            return "ISBN:" + book.getIsbn10();
        }
        return "BOOK:" + bookId;
    }

    private void runWithInflightLock(String keyPrefix, Long bookId, Task task) {
        if (bookId == null) {
            throw new IllegalArgumentException("bookId 不能为空");
        }

        String key = keyPrefix + bookId;
        boolean locked = tryAcquireInflightLock(key);
        if (!locked) {
            log.info("任务正在处理中，跳过重复消息: key={}", key);
            return;
        }

        try {
            task.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            releaseInflightLock(key);
        }
    }

    private boolean tryAcquireInflightLock(String key) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    "1",
                    Duration.ofMinutes(Math.max(inflightTtlMinutes, 1))
            );
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.warn("获取 RAG 任务幂等锁失败，继续执行任务: key={}", key, e);
            return true;
        }
    }

    private void releaseInflightLock(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("释放 RAG 任务幂等锁失败: key={}", key, e);
        }
    }

    private void safeUpdateEmbeddingStatus(Long bookId, int status) {
        try {
            Result<Void> result = remoteBookService.updateEmbeddingStatus(bookId, status);
            if (!isSuccess(result)) {
                log.warn("更新图书向量状态失败: bookId={}, status={}, code={}, msg={}",
                        bookId, status, result == null ? null : result.getCode(), result == null ? null : result.getMsg());
            }
        } catch (Exception e) {
            log.warn("更新图书向量状态失败: bookId={}, status={}", bookId, status, e);
        }
    }

    private void requireSuccess(Result<?> result, String message) {
        if (!isSuccess(result)) {
            throw new IllegalStateException(message + ", code="
                    + (result == null ? null : result.getCode()) + ", msg="
                    + (result == null ? null : result.getMsg()));
        }
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && result.getCode() == RESULT_SUCCESS_CODE;
    }

    @FunctionalInterface
    private interface Task {
        void run() throws Exception;
    }
}
