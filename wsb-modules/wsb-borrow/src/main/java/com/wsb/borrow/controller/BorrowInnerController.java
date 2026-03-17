package com.wsb.borrow.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.borrow.api.dto.BookBorrowCountDTO;
import com.wsb.borrow.api.dto.BorrowCategoryStatsDTO;
import com.wsb.borrow.api.dto.UserBorrowStatsDTO;
import com.wsb.borrow.domain.BookBorrow;
import com.wsb.borrow.service.BookBorrowService;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 借阅内部接口（供其他服务调用）
 */
@RestController
@RequestMapping("/v1/inner/borrow")
@RequiredArgsConstructor
public class BorrowInnerController {

    private final BookBorrowService bookBorrowService;

    /**
     * 统计书籍借阅次数（批量）
     */
    @GetMapping("/stats/book-count")
    public Result<List<BookBorrowCountDTO>> countBorrowByBooks(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowService.list(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2)); // 借出
        } else {
            borrows = bookBorrowService.list(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2));
        }

        // 按书籍分组统计
        Map<Long, Long> countMap = borrows.stream()
                .collect(Collectors.groupingBy(BookBorrow::getBookId, Collectors.counting()));

        List<BookBorrowCountDTO> result = countMap.entrySet().stream()
                .map(e -> {
                    BookBorrowCountDTO dto = new BookBorrowCountDTO();
                    dto.setBookId(e.getKey());
                    dto.setBorrowCount(e.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 统计用户的借阅数据（用户作为借阅者）
     */
    @GetMapping("/stats/user-borrowed")
    public Result<UserBorrowStatsDTO> getUserBorrowStats(@RequestParam("user_id") Long userId) {
        List<BookBorrow> borrows = bookBorrowService.list(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, userId)
                .eq(BookBorrow::getIsDeleted, false));

        int totalBorrowed = borrows.size();
        int unreturned = (int) borrows.stream()
                .filter(b -> b.getReturnTime() == null)
                .count();

        UserBorrowStatsDTO dto = new UserBorrowStatsDTO();
        dto.setTotalBorrowed(totalBorrowed);
        dto.setUnreturned(unreturned);

        return Result.success(dto);
    }

    /**
     * 统计书籍被借出未归还数（owner维度）
     */
    @GetMapping("/stats/owner-unreturned")
    public Result<Integer> countUnreturnedByOwner(@RequestParam("owner_id") Long ownerId) {
        // 通过 userId（借阅者）和 borrowType=2（借出）来统计
        // 这里需要关联 Book 表获取 ownerId，简化处理：直接查 userId=ownerId 且 borrowType=1（借入）的未归还数
        // 实际业务逻辑：owner的书被借出，需要在借阅记录中体现
        // 暂时返回0，待后续完善关联查询
        long count = bookBorrowService.count(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, ownerId)
                .eq(BookBorrow::getBorrowType, 2) // 借出
                .eq(BookBorrow::getIsDeleted, false)
                .isNull(BookBorrow::getReturnTime));

        return Result.success((int) count);
    }

    /**
     * 按分类统计借阅数据（指定书籍ID列表）
     * 注：由于借阅表没有分类字段，这里简化返回空列表
     */
    @GetMapping("/stats/category")
    public Result<List<BorrowCategoryStatsDTO>> getBorrowStatsByCategory(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        // 暂时返回空列表，需要关联Book表获取分类信息
        return Result.success(List.of());
    }

    /**
     * 获取书籍借阅统计（总数、阅读中、已读）
     */
    @GetMapping("/stats/summary")
    public Result<BorrowCategoryStatsDTO> getBorrowSummary(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowService.list(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false));
        } else {
            borrows = bookBorrowService.list(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false));
        }

        int total = borrows.size();
        int reading = (int) borrows.stream().filter(b -> b.getReturnTime() == null).count();
        int read = total - reading;

        BorrowCategoryStatsDTO dto = new BorrowCategoryStatsDTO();
        dto.setTotal(total);
        dto.setReading(reading);
        dto.setRead(read);

        return Result.success(dto);
    }
}