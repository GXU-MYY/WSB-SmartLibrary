package com.wsb.book.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.domain.Book;
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
@RequestMapping("/v1/inner/book")
@RequiredArgsConstructor
public class BookInnerController {

    private final BookService bookService;

    @GetMapping("/{bookId}")
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

    @GetMapping("/batch")
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

    // ========== 统计接口 ==========

    /**
     * 统计用户拥书数量（批量）
     */
    @GetMapping("/stats/user-count")
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
    @GetMapping("/stats/category")
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
    @GetMapping("/stats/user-books")
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
}