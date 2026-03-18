package com.wsb.book.api;

import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 书籍远程调用服务
 */
@FeignClient(value = "wsb-book", path = "/v1/inner")
public interface RemoteBookService {

    /**
     * 根据ID获取书籍信息
     */
    @GetMapping("/book/{bookId}")
    Result<BookRemoteDTO> getBookById(@PathVariable("bookId") Long bookId);

    /**
     * 根据ID列表批量获取书籍信息
     */
    @GetMapping("/book/batch")
    Result<List<BookRemoteDTO>> getBooksByIds(@RequestParam("ids") List<Long> bookIds);

    /**
     * 根据ID获取书架信息
     */
    @GetMapping("/shelf/{shelfId}")
    Result<ShelfRemoteDTO> getShelfById(@PathVariable("shelfId") Long shelfId);

    /**
     * 根据ID列表批量获取书架信息
     */
    @GetMapping("/shelf/batch")
    Result<List<ShelfRemoteDTO>> getShelfByIds(@RequestParam("ids") List<Long> shelfIds);

    // ========== 书籍统计相关接口 ==========

    /**
     * 统计用户拥书数量（批量）
     */
    @GetMapping("/book/stats/user-count")
    Result<List<UserBookCountDTO>> countBooksByUsers(@RequestParam("user_ids") List<Long> userIds);

    /**
     * 按分类统计书籍数量（指定用户）
     */
    @GetMapping("/book/stats/category")
    Result<List<CategoryCountDTO>> countBooksByCategory(@RequestParam("user_id") Long userId);

    /**
     * 获取用户拥有的书籍ID列表
     */
    @GetMapping("/book/stats/user-books")
    Result<List<Long>> getBookIdsByOwner(@RequestParam("user_id") Long userId);

    // ========== 借阅统计相关接口 ==========

    /**
     * 统计书籍借阅次数（批量）
     */
    @GetMapping("/borrow/stats/book-count")
    Result<List<BookBorrowCountDTO>> countBorrowByBooks(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 统计用户的借阅数据（用户作为借阅者）
     */
    @GetMapping("/borrow/stats/user-borrowed")
    Result<UserBorrowStatsDTO> getUserBorrowStats(@RequestParam("user_id") Long userId);

    /**
     * 统计书籍被借出未归还数（owner维度）
     */
    @GetMapping("/borrow/stats/owner-unreturned")
    Result<Integer> countUnreturnedByOwner(@RequestParam("owner_id") Long ownerId);

    /**
     * 按分类统计借阅数据（指定书籍ID列表）
     */
    @GetMapping("/borrow/stats/category")
    Result<List<BorrowCategoryStatsDTO>> getBorrowStatsByCategory(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 获取书籍借阅统计（总数、阅读中、已读）
     */
    @GetMapping("/borrow/stats/summary")
    Result<BorrowCategoryStatsDTO> getBorrowSummary(@RequestParam("book_ids") List<Long> bookIds);
}