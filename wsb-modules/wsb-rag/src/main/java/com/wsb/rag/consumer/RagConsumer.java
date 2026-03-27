package com.wsb.rag.consumer;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.EmbeddingService;
import com.wsb.rag.service.SummaryService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagConsumer {

    private final RemoteBookService remoteBookService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final SummaryService summaryService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @RabbitListener(queues = "${rag.queue.summary}")
    public void processSummary(Long bookId) {
        log.info("开始生成摘要: bookId={}", bookId);
        try {
            String summary = summaryService.generateSummary(bookId);
            if (StringUtils.isBlank(summary)) {
                log.warn("摘要生成结果为空，跳过向量任务: bookId={}", bookId);
                return;
            }
            rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
            log.info("摘要生成完成并已发送向量任务: bookId={}", bookId);
        } catch (Exception e) {
            log.error("摘要生成失败: bookId={}", bookId, e);
        }
    }

    @RabbitListener(queues = "${rag.queue.embedding}")
    public void processEmbedding(Long bookId) {
        log.info("开始生成向量: bookId={}", bookId);
        try {
            remoteBookService.updateEmbeddingStatus(bookId, 1);

            Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
            BookRemoteDTO book = result.getData();
            if (book == null) {
                log.warn("书籍不存在: bookId={}", bookId);
                return;
            }

            String text = buildEmbeddingText(book);
            var embedding = embeddingService.generateEmbedding(text);
            vectorService.storeEmbedding(bookId, embedding, book);
            remoteBookService.updateEmbeddingStatus(bookId, 2);

            log.info("向量生成完成: bookId={}", bookId);
        } catch (Exception e) {
            log.error("向量生成失败: bookId={}", bookId, e);
            remoteBookService.updateEmbeddingStatus(bookId, 0);
        }
    }

    private String buildEmbeddingText(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(book.getTitle())) {
            sb.append(book.getTitle()).append(" ");
        }
        if (StringUtils.isNotBlank(book.getSubtitle())) {
            sb.append(book.getSubtitle()).append(" ");
        }
        if (StringUtils.isNotBlank(book.getAuthor())) {
            sb.append(book.getAuthor()).append(" ");
        }
        if (StringUtils.isNotBlank(book.getKeyword())) {
            sb.append(book.getKeyword()).append(" ");
        }
        if (StringUtils.isNotBlank(book.getSummary())) {
            sb.append(book.getSummary());
        }
        return sb.toString().trim();
    }
}
