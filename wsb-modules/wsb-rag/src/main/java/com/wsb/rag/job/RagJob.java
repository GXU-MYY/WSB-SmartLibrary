package com.wsb.rag.job;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagJob {

    private final RemoteBookService remoteBookService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    /**
     * 定时处理空摘要图书，每小时执行。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processNullSummary() {
        log.info("开始处理空摘要图书");
        Result<List<Long>> result = remoteBookService.getBooksWithNullSummary();
        List<Long> bookIds = result.getData();

        if (bookIds == null || bookIds.isEmpty()) {
            log.info("没有需要处理的空摘要图书");
            return;
        }

        log.info("发现 {} 本空摘要图书", bookIds.size());
        for (Long bookId : bookIds) {
            rabbitTemplate.convertAndSend(exchange, summaryRoutingKey, bookId);
        }
    }

    /**
     * 定时处理未生成向量的图书，每 2 小时执行。
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void processPendingEmbedding() {
        log.info("开始处理未生成向量的图书");
        Result<List<Long>> result = remoteBookService.getBooksPendingEmbedding();
        List<Long> bookIds = result.getData();

        if (bookIds == null || bookIds.isEmpty()) {
            log.info("没有需要处理的向量图书");
            return;
        }

        log.info("发现 {} 本未生成向量的图书", bookIds.size());
        for (Long bookId : bookIds) {
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
        }
    }
}
