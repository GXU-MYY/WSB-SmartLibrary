package com.wsb.rag.service.impl;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final int OWNED_RECOMMEND_MIN_CANDIDATES = 60;
    private static final int OWNED_RECOMMEND_MULTIPLIER = 6;

    private final RemoteBookService remoteBookService;
    private final VectorService vectorService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Override
    public List<BookRemoteDTO> recommend(String query, int limit, Long ownerId) {
        List<Long> bookIds = ownerId == null
                ? vectorService.searchSimilar(query, limit)
                : searchOwnedBooks(query, limit, ownerId);
        if (bookIds.isEmpty()) {
            return List.of();
        }

        return fetchBooksInOrder(bookIds);
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
        log.info("已发送摘要生成任务, bookId={}", bookId);
    }

    @Override
    public void processNewBook(Long bookId) {
        enqueueSummary(bookId);
    }

    public void enqueueEmbedding(Long bookId) {
        rabbitTemplate.convertAndSend(exchange, embeddingRoutingKey, bookId);
        log.info("已发送向量生成任务, bookId={}", bookId);
    }

    private List<Long> searchOwnedBooks(String query, int limit, Long ownerId) {
        Result<List<Long>> ownedBookIdsResult = remoteBookService.getBookIdsByOwner(ownerId);
        List<Long> ownedBookIds = ownedBookIdsResult.getData();
        if (ownedBookIds == null || ownedBookIds.isEmpty()) {
            return List.of();
        }

        Set<Long> ownedBookIdSet = Set.copyOf(ownedBookIds);
        int candidateLimit = Math.max(OWNED_RECOMMEND_MIN_CANDIDATES, limit * OWNED_RECOMMEND_MULTIPLIER);

        return vectorService.searchSimilar(query, candidateLimit).stream()
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
}
