package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.rag.dto.RecommendedBookPreviewDTO;

import java.util.List;

public interface RagService {

    List<BookRemoteDTO> recommend(String query, int limit, Long ownerId);

    List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit);

    RecommendedBookPreviewDTO getRecommendedBookPreview(Long bookId);

    void enqueueSummary(Long bookId);

    void enqueueEmbedding(Long bookId);

    int requeueDeadLetters(String taskType, int limit);

}
