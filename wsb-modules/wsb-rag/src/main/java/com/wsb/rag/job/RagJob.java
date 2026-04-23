package com.wsb.rag.job;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * RAG 定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagJob {

    private static final String SUMMARY_JOB_LOCK_KEY = "rag:job:summary";
    private static final String EMBEDDING_JOB_LOCK_KEY = "rag:job:embedding";

    private final RemoteBookService remoteBookService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final String lockOwner = UUID.randomUUID().toString();

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.job.lock-ttl-minutes:55}")
    private long jobLockTtlMinutes;

    @Value("${rag.job.batch-size:100}")
    private int jobBatchSize;

    @Value("${rag.job.dispatch-interval-ms:100}")
    private long dispatchIntervalMs;

    /**
     * 定时处理空摘要图书，每小时执行。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processNullSummary() {
        if (!tryAcquireJobLock(SUMMARY_JOB_LOCK_KEY)) {
            log.info("空摘要定时任务已有其他节点在执行，本节点跳过");
            return;
        }

        try {
            log.info("开始处理空摘要图书");
            Result<List<Long>> result = remoteBookService.getBooksWithNullSummary();
            List<Long> bookIds = result.getData();

            if (bookIds == null || bookIds.isEmpty()) {
                log.info("没有需要处理的空摘要图书");
                return;
            }

            int sentCount = dispatchBookTasks(bookIds, summaryRoutingKey);
            log.info("发现 {} 本空摘要图书，本轮已投递 {} 个摘要任务", bookIds.size(), sentCount);
        } finally {
            releaseJobLock(SUMMARY_JOB_LOCK_KEY);
        }
    }

    /**
     * 定时处理未生成向量的图书，每 2 小时执行。
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void processPendingEmbedding() {
        if (!tryAcquireJobLock(EMBEDDING_JOB_LOCK_KEY)) {
            log.info("向量定时任务已有其他节点在执行，本节点跳过");
            return;
        }

        try {
            log.info("开始处理未生成向量的图书");
            Result<List<Long>> result = remoteBookService.getBooksPendingEmbedding();
            List<Long> bookIds = result.getData();

            if (bookIds == null || bookIds.isEmpty()) {
                log.info("没有需要处理的向量图书");
                return;
            }

            int sentCount = 0;
            int checkedCount = 0;
            int batchSize = resolveJobBatchSize();
            log.info("发现 {} 本未生成向量的图书，本轮最多投递 {} 个向量任务", bookIds.size(), batchSize);
            for (Long bookId : bookIds) {
                if (sentCount >= batchSize) {
                    break;
                }
                if (bookId == null) {
                    continue;
                }

                checkedCount++;
                Result<BookRemoteDTO> bookResult = remoteBookService.getBookById(bookId);
                BookRemoteDTO book = bookResult.getData();
                if (book == null) {
                    log.warn("图书不存在，跳过向量生成任务: bookId={}", bookId);
                    continue;
                }
                if (StringUtils.isBlank(book.getSummary())) {
                    log.warn("图书摘要为空，跳过向量生成任务: bookId={}", bookId);
                    continue;
                }

                rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
                sentCount++;
                sleepDispatchInterval();
            }
            log.info("本轮检查 {} 本图书，已投递 {} 个向量任务", checkedCount, sentCount);
        } finally {
            releaseJobLock(EMBEDDING_JOB_LOCK_KEY);
        }
    }

    private int dispatchBookTasks(List<Long> bookIds, String routingKey) {
        int sentCount = 0;
        int batchSize = resolveJobBatchSize();
        for (Long bookId : bookIds) {
            if (sentCount >= batchSize) {
                break;
            }
            if (bookId == null) {
                continue;
            }
            rabbitTemplate.convertAndSend(exchange, routingKey, bookId);
            sentCount++;
            sleepDispatchInterval();
        }
        return sentCount;
    }

    private boolean tryAcquireJobLock(String key) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    lockOwner,
                    Duration.ofMinutes(Math.max(jobLockTtlMinutes, 1))
            );
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.warn("获取 RAG 定时任务锁失败，继续执行任务: key={}", key, e);
            return true;
        }
    }

    private void releaseJobLock(String key) {
        try {
            String owner = stringRedisTemplate.opsForValue().get(key);
            if (lockOwner.equals(owner)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("释放 RAG 定时任务锁失败: key={}", key, e);
        }
    }

    private int resolveJobBatchSize() {
        return Math.max(jobBatchSize, 1);
    }

    private void sleepDispatchInterval() {
        long interval = Math.max(dispatchIntervalMs, 0);
        if (interval <= 0) {
            return;
        }
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RAG 定时任务投递被中断", e);
        }
    }
}
