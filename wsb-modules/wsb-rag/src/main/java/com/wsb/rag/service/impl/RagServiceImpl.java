package com.wsb.rag.service.impl;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.EmbeddingService;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final RemoteBookService remoteBookService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Override
    public List<BookRemoteDTO> recommend(String query, int limit) {
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
        List<Long> bookIds = vectorService.searchSimilar(queryEmbedding, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        return result.getData() != null ? result.getData() : List.of();
    }

    @Override
    public List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit) {
        List<Long> bookIds = vectorService.getSimilarBooks(bookId, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        return result.getData() != null ? result.getData() : List.of();
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
}
