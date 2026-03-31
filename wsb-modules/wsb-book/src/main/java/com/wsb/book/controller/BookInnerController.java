package com.wsb.book.controller;

import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.book.service.BookInnerService;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书内部接口，供其他服务调用
 */
@RestController
@RequestMapping("/v1/inner")
@RequiredArgsConstructor
public class BookInnerController {

    private final BookInnerService bookInnerService;

    @GetMapping("/book/{bookId}")
    public Result<BookRemoteDTO> getBookById(@PathVariable Long bookId) {
        return Result.success(bookInnerService.getBookById(bookId));
    }

    @GetMapping("/book/batch")
    public Result<List<BookRemoteDTO>> getBooksByIds(@RequestParam(value = "ids", required = false) List<Long> bookIds) {
        return Result.success(bookInnerService.getBooksByIds(bookIds));
    }

    @GetMapping("/book/stats/user-count")
    public Result<List<UserBookCountDTO>> countBooksByUsers(@RequestParam(value = "user_ids", required = false) List<Long> userIds) {
        return Result.success(bookInnerService.countBooksByUsers(userIds));
    }

    @GetMapping("/book/stats/category")
    public Result<List<CategoryCountDTO>> countBooksByCategory(@RequestParam("user_id") Long userId) {
        return Result.success(bookInnerService.countBooksByCategory(userId));
    }

    @GetMapping("/book/stats/user-books")
    public Result<List<Long>> getBookIdsByOwner(@RequestParam("user_id") Long userId) {
        return Result.success(bookInnerService.getBookIdsByOwner(userId));
    }

    @GetMapping("/borrow/stats/book-count")
    public Result<List<BookBorrowCountDTO>> countBorrowByBooks(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        return Result.success(bookInnerService.countBorrowByBooks(bookIds));
    }

    @GetMapping("/borrow/stats/user-borrowed")
    public Result<UserBorrowStatsDTO> getUserBorrowStats(@RequestParam("user_id") Long userId) {
        return Result.success(bookInnerService.getUserBorrowStats(userId));
    }

    @GetMapping("/borrow/stats/owner-unreturned")
    public Result<Integer> countUnreturnedByOwner(@RequestParam("owner_id") Long ownerId) {
        return Result.success(bookInnerService.countUnreturnedByOwner(ownerId));
    }

    @GetMapping("/borrow/stats/category")
    public Result<List<BorrowCategoryStatsDTO>> getBorrowStatsByCategory(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        return Result.success(bookInnerService.getBorrowStatsByCategory(bookIds));
    }

    @GetMapping("/borrow/stats/summary")
    public Result<BorrowCategoryStatsDTO> getBorrowSummary(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        return Result.success(bookInnerService.getBorrowSummary(bookIds));
    }

    @GetMapping("/book/rag/summary-null")
    public Result<List<Long>> getBooksWithNullSummary() {
        return Result.success(bookInnerService.getBooksWithNullSummary());
    }

    @GetMapping("/book/rag/embedding-pending")
    public Result<List<Long>> getBooksPendingEmbedding() {
        return Result.success(bookInnerService.getBooksPendingEmbedding());
    }

    @PutMapping("/book/rag/{bookId}/summary")
    public Result<Void> updateSummary(@PathVariable Long bookId, @RequestParam("summary") String summary) {
        bookInnerService.updateSummary(bookId, summary);
        return Result.success();
    }

    @PutMapping("/book/rag/{bookId}/embedding-status")
    public Result<Void> updateEmbeddingStatus(@PathVariable Long bookId, @RequestParam("status") Integer status) {
        bookInnerService.updateEmbeddingStatus(bookId, status);
        return Result.success();
    }
}
