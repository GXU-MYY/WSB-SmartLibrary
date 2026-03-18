package com.wsb.book.controller;

import com.wsb.book.api.dto.BookReadingAddDTO;
import com.wsb.book.api.dto.BookReadingUpdateDTO;
import com.wsb.book.api.vo.BookReadingVO;
import com.wsb.book.service.ReadingService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 阅读记录控制器
 */
@Tag(name = "阅读记录管理")
@RestController
@RequestMapping("/v1/book/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @Operation(summary = "新增阅读记录")
    @PostMapping
    public Result<BookReadingVO> add(@Valid @RequestBody BookReadingAddDTO dto) {
        return Result.success(readingService.add(dto));
    }

    @Operation(summary = "查询阅读记录")
    @GetMapping
    public Result<?> get(
            @Parameter(description = "图书ID，不传则查询当前用户所有阅读记录")
            @RequestParam(value = "book_id", required = false) Long bookId) {
        if (bookId != null) {
            return Result.success(readingService.getByBookId(bookId));
        }
        return Result.success(readingService.listAll());
    }

    @Operation(summary = "更新阅读记录")
    @PutMapping
    public Result<BookReadingVO> update(@Valid @RequestBody BookReadingUpdateDTO dto) {
        return Result.success(readingService.update(dto));
    }
}