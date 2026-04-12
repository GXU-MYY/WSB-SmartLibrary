package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;

import java.util.List;

public interface RagService {

    List<BookRemoteDTO> recommend(String query, int limit);

    List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit);

    void enqueueSummary(Long bookId);

    void processNewBook(Long bookId);
}
