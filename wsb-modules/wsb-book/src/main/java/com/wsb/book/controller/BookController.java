package com.wsb.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookShelfDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.api.vo.MyBookListVO;
import com.wsb.book.api.vo.RecentBookVO;
import com.wsb.book.api.vo.ShelfVO;
import com.wsb.book.service.BookService;
import com.wsb.book.service.ShelfService;
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
  private final ShelfService shelfService;

  @Operation(summary = "我的书籍列表")
  @GetMapping("/my")
  public Result<MyBookListVO> myBooks() {
    return Result.success(bookService.getMyBooks());
  }

  @Operation(summary = "最近整理的图书")
  @GetMapping("/recent")
  public Result<List<RecentBookVO>> recentBooks() {
    return Result.success(bookService.getRecentBooks());
  }

  @Operation(summary = "书籍查询")
  @GetMapping
  public Result<Page<BookVO>> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                   @RequestParam(value = "page_size", defaultValue = "10") Integer page_size,
                                   @RequestParam(value = "bookshelf_id", required = false) Long bookshelfId,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "classify", required = false) String classify) {
    return Result.success(bookService.searchByCondition(page, page_size, bookshelfId, keyword, classify));
  }

  @Operation(summary = "书籍详情")
  @GetMapping("/detail")
  public Result<BookVO> detail(@RequestParam("book_id") Long bookId) {
    return Result.success(bookService.detail(bookId));
  }

  @Operation(summary = "查询图书所在书架")
  @GetMapping("/shelf")
  public Result<ShelfVO> bookShelves(@RequestParam("book_id") Long bookId) {
    return Result.success(shelfService.getByBookId(bookId));
  }

  @Operation(summary = "新增书籍")
  @PostMapping
  public Result<BookAddVO> add(@RequestBody BookAddDTO book) {
    return Result.success(bookService.add(book));
  }

  @Operation(summary = "修改书籍")
  @PutMapping
  public Result<BookVO> update(@Valid @RequestBody BookUpdateDTO dto) {
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
}
