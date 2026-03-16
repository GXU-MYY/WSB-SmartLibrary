package com.wsb.book.controller;

import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.service.BookService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ISBN查询")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class IsbnController {

    private final BookService bookService;

    @Operation(summary = "ISBN查询信息")
    @GetMapping("/isbn")
    public Result<IsbnBookVO> getBookByIsbn(@RequestParam("isbn") String isbn) {
        return Result.success(bookService.getBookByIsbn(isbn));
    }
}