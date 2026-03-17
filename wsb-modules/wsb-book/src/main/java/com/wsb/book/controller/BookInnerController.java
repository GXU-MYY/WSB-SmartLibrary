package com.wsb.book.controller;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.domain.Book;
import com.wsb.book.service.BookService;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
}