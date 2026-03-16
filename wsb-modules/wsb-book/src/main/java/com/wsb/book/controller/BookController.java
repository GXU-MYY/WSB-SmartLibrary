package com.wsb.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookShelfDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.api.vo.MyBookListVO;
import com.wsb.book.service.BookBorrowService;
import com.wsb.book.service.BookService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "书籍管理")
@RestController
@RequestMapping("/v1/book")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;
  private final BookBorrowService bookBorrowService;

  @Operation(summary = "我的书籍列表")
  @GetMapping("/my")
  public Result<MyBookListVO> myBooks() {
    return Result.success(bookService.getMyBooks());
  }

  @Operation(summary = "书籍按书架查询")
  @GetMapping
  public Result<Page<BookVO>> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                   @RequestParam(value = "page_size", defaultValue = "10") Integer page_size,
                                   @RequestParam(value = "bookshelf_id", required = false) String bookshelfId,
                                   @RequestParam(value = "book_name", required = false) String bookName,
                                   @RequestParam(value = "classify", required = false) String classify) {
    return Result.success(bookService.searchByCondition(page, page_size, bookshelfId, bookName, classify));
  }

  @Operation(summary = "书籍详情")
  @GetMapping("/detail")
  public Result<BookVO> detail(@RequestParam("book_id") Long bookId) {
    return Result.success(bookService.detail(bookId));
  }

  @Operation(summary = "新增书籍")
  @PostMapping
  public Result<BookAddVO> add(@RequestBody BookAddDTO book) {
    return Result.success(bookService.add(book));
  }

  @Operation(summary = "修改书籍")
  @PutMapping
  public Result<BookVO> update(@RequestBody BookUpdateDTO dto) {
    return Result.success(bookService.update(dto));
  }

  @Operation(summary = "删除书籍")
  @DeleteMapping
  public Result<Void> delete(@RequestParam("id") Long id) {
    bookService.delete(id);
    return Result.success();
  }

  @Operation(summary = "书籍上架")
  @PostMapping("/shelf")
  public Result<Void> onShelf(@RequestBody BookShelfDTO dto) {
    bookService.onShelf(dto);
    return Result.success();
  }

  @Operation(summary = "书籍下架")
  @DeleteMapping("/shelf")
  public Result<Void> offShelf(@RequestBody BookShelfDTO dto) {
    bookService.offShelf(dto);
    return Result.success();
  }

  // ==================== 借书相关接口 ====================

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