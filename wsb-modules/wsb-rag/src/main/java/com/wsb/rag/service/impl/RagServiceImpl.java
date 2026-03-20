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

/**
 * RAG 服务实现
 */
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
        // 生成查询向量
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);

        // 向量搜索
        List<Long> bookIds = vectorService.searchSimilar(queryEmbedding, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        // 获取书籍详情
        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        return result.getData() != null ? result.getData() : List.of();
    }

    @Override
    public List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit) {
        // 向量搜索相似书籍
        List<Long> bookIds = vectorService.getSimilarBooks(bookId, limit);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        // 获取书籍详情
        Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
        return result.getData() != null ? result.getData() : List.of();
    }

    @Override
    public void processNewBook(Long bookId) {
        // 发送到消息队列异步处理
        rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
        rabbitTemplate.convertAndSend(exchange, summaryRoutingKey, bookId);
        log.info("已发送新书处理任务: bookId={}", bookId);
    }
}