package com.wsb.borrow.controller;

import com.wsb.borrow.api.dto.BookBorrowDTO;
import com.wsb.borrow.api.dto.BookBorrowUpdateDTO;
import com.wsb.borrow.api.dto.BookReturnDTO;
import com.wsb.borrow.api.vo.BookBorrowRecordVO;
import com.wsb.borrow.api.vo.BookBorrowVO;
import com.wsb.borrow.service.BookBorrowService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "借阅管理")
@RestController
@RequestMapping("/v1/book")
@RequiredArgsConstructor
public class BookBorrowController {

    private final BookBorrowService bookBorrowService;

    @Operation(summary = "书籍借阅")
    @PostMapping("/borrow")
    public Result<BookBorrowVO> borrow(@Valid @RequestBody BookBorrowDTO dto) {
        return Result.success(bookBorrowService.borrow(dto));
    }

    @Operation(summary = "还书")
    @PostMapping("/returning")
    public Result<BookBorrowVO> returning(@Valid @RequestBody BookReturnDTO dto) {
        return Result.success(bookBorrowService.returning(dto));
    }

    @Operation(summary = "借书记录")
    @GetMapping("/borrow")
    public Result<List<BookBorrowRecordVO>> getBorrowRecords(
            @RequestParam(value = "borrow_type", required = false) Integer borrowType) {
        return Result.success(bookBorrowService.getRecords(borrowType));
    }

    @Operation(summary = "修改借书信息")
    @PutMapping("/borrow")
    public Result<Void> updateBorrow(@Valid @RequestBody BookBorrowUpdateDTO dto) {
        bookBorrowService.updateBorrow(dto);
        return Result.success();
    }
}