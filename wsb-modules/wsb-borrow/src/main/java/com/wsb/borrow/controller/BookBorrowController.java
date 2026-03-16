package com.wsb.borrow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.borrow.domain.BookBorrow;
import com.wsb.borrow.service.BookBorrowService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "借阅管理")
@RestController
@RequestMapping("/v1/admin/borrow")
@RequiredArgsConstructor
public class BookBorrowController {

  private final BookBorrowService bookBorrowService;

  @Operation(summary = "查询借阅记录")
  @GetMapping
  public Result<Page<BookBorrow>> list(@RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer page_size,
      @RequestParam(required = false) Long user_id) {
    Page<BookBorrow> pageParam = new Page<>(page, page_size);
    LambdaQueryWrapper<BookBorrow> wrapper = new LambdaQueryWrapper<>();
    if (user_id != null) {
      wrapper.eq(BookBorrow::getUserId, user_id);
    }
    return Result.success(bookBorrowService.page(pageParam, wrapper));
  }

  @Operation(summary = "新增借阅记录")
  @PostMapping
  public Result<BookBorrow> add(@RequestBody BookBorrow bookBorrow) {
    bookBorrowService.save(bookBorrow);
    return Result.success(bookBorrow);
  }
}
