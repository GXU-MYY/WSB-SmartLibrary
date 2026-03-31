package com.wsb.book.service;

import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;

import java.util.List;

/**
 * 图书内部服务
 */
public interface BookInnerService {

    BookRemoteDTO getBookById(Long bookId);

    List<BookRemoteDTO> getBooksByIds(List<Long> bookIds);

    List<UserBookCountDTO> countBooksByUsers(List<Long> userIds);

    List<CategoryCountDTO> countBooksByCategory(Long userId);

    List<Long> getBookIdsByOwner(Long userId);

    List<BookBorrowCountDTO> countBorrowByBooks(List<Long> bookIds);

    UserBorrowStatsDTO getUserBorrowStats(Long userId);

    Integer countUnreturnedByOwner(Long ownerId);

    List<BorrowCategoryStatsDTO> getBorrowStatsByCategory(List<Long> bookIds);

    BorrowCategoryStatsDTO getBorrowSummary(List<Long> bookIds);

    List<Long> getBooksWithNullSummary();

    List<Long> getBooksPendingEmbedding();

    void updateSummary(Long bookId, String summary);

    void updateEmbeddingStatus(Long bookId, Integer status);
}
