package com.wsb.borrow.api;

import com.wsb.borrow.api.dto.BookBorrowCountDTO;
import com.wsb.borrow.api.dto.BorrowCategoryStatsDTO;
import com.wsb.borrow.api.dto.UserBorrowStatsDTO;
import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 借阅远程调用服务
 */
@FeignClient(value = "wsb-borrow", path = "/v1/inner")
public interface RemoteBorrowService {

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