package com.wsb.book.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.service.BookService;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 书籍内部接口（供其他服务调用）
 */
@RestController
@RequestMapping("/v1/inner")
@RequiredArgsConstructor
public class BookInnerController {

    private final BookService bookService;
    private final BookBorrowMapper bookBorrowMapper;

    // ========== 书籍接口 ==========

    @GetMapping("/book/{bookId}")
    public Result<BookRemoteDTO> getBookById(@PathVariable Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            return Result.success(null);
        }
        BookRemoteDTO dto = new BookRemoteDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setCoverUrl(book.getCoverUrl());
        return Result.success(dto);
    }

    @GetMapping("/book/batch")
    public Result<List<BookRemoteDTO>> getBooksByIds(@RequestParam(value = "ids", required = false) List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Book> books = bookService.listByIds(bookIds);
        List<BookRemoteDTO> dtos = books.stream().map(book -> {
            BookRemoteDTO dto = new BookRemoteDTO();
            dto.setId(book.getId());
            dto.setTitle(book.getTitle());
            dto.setCoverUrl(book.getCoverUrl());
            return dto;
        }).collect(Collectors.toList());
        return Result.success(dtos);
    }

    // ========== 书籍统计接口 ==========

    /**
     * 统计用户拥书数量（批量）
     */
    @GetMapping("/book/stats/user-count")
    public Result<List<UserBookCountDTO>> countBooksByUsers(@RequestParam(value = "user_ids", required = false) List<Long> userIds) {
        List<Book> books;
        if (userIds != null && !userIds.isEmpty()) {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .in(Book::getUserId, userIds)
                    .eq(Book::getIsDeleted, false));
        } else {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .eq(Book::getIsDeleted, false));
        }

        // 按用户分组统计
        Map<Long, Long> countMap = books.stream()
                .collect(Collectors.groupingBy(Book::getUserId, Collectors.counting()));

        List<UserBookCountDTO> result = countMap.entrySet().stream()
                .map(e -> {
                    UserBookCountDTO dto = new UserBookCountDTO();
                    dto.setUserId(e.getKey());
                    dto.setBookCount(e.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 按分类统计书籍数量（指定用户）
     */
    @GetMapping("/book/stats/category")
    public Result<List<CategoryCountDTO>> countBooksByCategory(@RequestParam("user_id") Long userId) {
        List<Book> books = bookService.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, userId)
                .eq(Book::getIsDeleted, false));

        // 按分类分组统计
        Map<String, Long> countMap = books.stream()
                .filter(b -> b.getClassify() != null && !b.getClassify().isEmpty())
                .collect(Collectors.groupingBy(Book::getClassify, Collectors.counting()));

        List<CategoryCountDTO> result = countMap.entrySet().stream()
                .map(e -> {
                    CategoryCountDTO dto = new CategoryCountDTO();
                    dto.setCategory(e.getKey());
                    dto.setCount(e.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取用户拥有的书籍ID列表
     */
    @GetMapping("/book/stats/user-books")
    public Result<List<Long>> getBookIdsByOwner(@RequestParam("user_id") Long userId) {
        List<Book> books = bookService.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, userId)
                .eq(Book::getIsDeleted, false)
                .select(Book::getId));

        List<Long> bookIds = books.stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        return Result.success(bookIds);
    }

    // ========== 借阅统计接口 ==========

    /**
     * 统计书籍借阅次数（批量）
     */
    @GetMapping("/borrow/stats/book-count")
    public Result<List<BookBorrowCountDTO>> countBorrowByBooks(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2)); // 借出
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
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
    @GetMapping("/borrow/stats/user-borrowed")
    public Result<UserBorrowStatsDTO> getUserBorrowStats(@RequestParam("user_id") Long userId) {
        List<BookBorrow> borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
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
    @GetMapping("/borrow/stats/owner-unreturned")
    public Result<Integer> countUnreturnedByOwner(@RequestParam("owner_id") Long ownerId) {
        long count = bookBorrowMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
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
    @GetMapping("/borrow/stats/category")
    public Result<List<BorrowCategoryStatsDTO>> getBorrowStatsByCategory(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        // 暂时返回空列表，需要关联Book表获取分类信息
        return Result.success(List.of());
    }

    /**
     * 获取书籍借阅统计（总数、阅读中、已读）
     */
    @GetMapping("/borrow/stats/summary")
    public Result<BorrowCategoryStatsDTO> getBorrowSummary(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
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