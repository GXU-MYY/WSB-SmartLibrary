package com.wsb.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowSummaryVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.service.BookBorrowService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅管理控制器
 */
@Tag(name = "借阅管理")
@RestController
@RequestMapping("/v1/book")
@RequiredArgsConstructor
public class BookBorrowController {

    private final BookBorrowService bookBorrowService;

    @Operation(summary = "图书借阅")
    @PostMapping("/borrow")
    public Result<BookBorrowVO> borrow(@Valid @RequestBody BookBorrowDTO dto) {
        return Result.success(bookBorrowService.borrow(dto));
    }

    @Operation(summary = "还书")
    @PostMapping("/returning")
    public Result<BookBorrowVO> returning(@Valid @RequestBody BookReturnDTO dto) {
        return Result.success(bookBorrowService.returning(dto));
    }

    @Operation(summary = "借阅记录")
    @GetMapping("/borrow")
    public Result<Page<BookBorrowRecordVO>> getBorrowRecords(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "borrow_type", required = false) Integer borrowType,
            @RequestParam(value = "status", required = false) Integer status) {
        return Result.success(bookBorrowService.getRecords(page, pageSize, borrowType, status));
    }

    @Operation(summary = "借阅汇总")
    @GetMapping("/borrow/summary")
    public Result<BookBorrowSummaryVO> getBorrowSummary() {
        return Result.success(bookBorrowService.getSummary());
    }

    @Operation(summary = "修改借阅信息")
    @PutMapping("/borrow")
    public Result<Void> updateBorrow(@Valid @RequestBody BookBorrowUpdateDTO dto) {
        bookBorrowService.updateBorrow(dto);
        return Result.success();
    }
}
