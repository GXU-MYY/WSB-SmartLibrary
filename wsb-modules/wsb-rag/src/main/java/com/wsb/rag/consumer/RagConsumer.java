package com.wsb.rag.consumer;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.EmbeddingService;
import com.wsb.rag.service.SummaryService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RAG 消息消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagConsumer {

    private final RemoteBookService remoteBookService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final SummaryService summaryService;

    /**
     * 消费摘要生成任务
     */
    @RabbitListener(queues = "${rag.queue.summary}")
    public void processSummary(Long bookId) {
        log.info("开始生成摘要: bookId={}", bookId);
        try {
            String summary = summaryService.generateSummary(bookId);
            log.info("摘要生成完成: bookId={}, summary={}", bookId, summary.substring(0, Math.min(50, summary.length())));
        } catch (Exception e) {
            log.error("摘要生成失败: bookId={}", bookId, e);
        }
    }

    /**
     * 消费向量生成任务
     */
    @RabbitListener(queues = "${rag.queue.embedding}")
    public void processEmbedding(Long bookId) {
        log.info("开始生成向量: bookId={}", bookId);
        try {
            // 更新状态为处理中
            remoteBookService.updateEmbeddingStatus(bookId, 1);

            // 获取书籍信息
            Result<BookRemoteDTO> result = remoteBookService.getBookById(bookId);
            BookRemoteDTO book = result.getData();
            if (book == null) {
                log.warn("书籍不存在: bookId={}", bookId);
                return;
            }

            // 构建文本用于向量化
            String text = buildEmbeddingText(book);

            // 生成向量
            var embedding = embeddingService.generateEmbedding(text);

            // 存储向量
            vectorService.storeEmbedding(bookId, embedding, book);

            // 更新状态为已完成
            remoteBookService.updateEmbeddingStatus(bookId, 2);

            log.info("向量生成完成: bookId={}", bookId);
        } catch (Exception e) {
            log.error("向量生成失败: bookId={}", bookId, e);
            // 更新状态为未处理（允许重试）
            remoteBookService.updateEmbeddingStatus(bookId, 0);
        }
    }

    private String buildEmbeddingText(BookRemoteDTO book) {
        StringBuilder sb = new StringBuilder();
        if (book.getTitle() != null) {
            sb.append(book.getTitle()).append(" ");
        }
        if (book.getSubtitle() != null) {
            sb.append(book.getSubtitle()).append(" ");
        }
        if (book.getAuthor() != null) {
            sb.append(book.getAuthor()).append(" ");
        }
        if (book.getKeyword() != null) {
            sb.append(book.getKeyword()).append(" ");
        }
        if (book.getLabel() != null) {
            sb.append(book.getLabel()).append(" ");
        }
        if (book.getSummary() != null) {
            sb.append(book.getSummary());
        }
        return sb.toString().trim();
    }
}