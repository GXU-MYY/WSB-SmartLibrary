package com.wsb.book.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.ShelfAddDTO;
import com.wsb.book.api.dto.ShelfDeleteDTO;
import com.wsb.book.api.dto.ShelfUpdateDTO;
import com.wsb.book.api.vo.ShelfVO;
import com.wsb.book.domain.Shelf;
import com.wsb.book.service.ShelfService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "书架管理")
@RestController
@RequestMapping("/v1/bookshelf")
@RequiredArgsConstructor
public class ShelfController {

  private final ShelfService shelfService;

  // todo
  @Operation(summary = "查询书架列表")
  @GetMapping("/list")
  public Result<Page<ShelfVO>> list(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer page_size,
      @RequestParam(name = "shelf_type", required = false) String shelfType,
      @RequestParam(name = "shelf_name", required = false) String shelfName,
      @RequestParam(name = "user_id", required = false) Long userId) {
    return Result.success(shelfService.list(page, page_size, shelfType, shelfName, userId));
  }

  @Operation(summary = "查询书架信息")
  @GetMapping
  public Result<List<ShelfVO>> detail(@RequestParam(value = "user_id", required = false) Long userId,
                                      @RequestParam(value = "id", required = false) Long id) {
    return Result.success(shelfService.detail(userId, id));
  }

  @Operation(summary = "添加书架信息")
  @PostMapping
  public Result<ShelfVO> add(@RequestBody ShelfAddDTO dto) {
    ShelfVO vo = shelfService.add(dto);
    return Result.success(vo);
  }

  @Operation(summary = "修改书架信息")
  @PutMapping
  public Result<ShelfVO> update(@RequestBody ShelfUpdateDTO dto) {
    ShelfVO shelf = shelfService.update(dto);
    return Result.success(shelf);
  }

  @Operation(summary = "删除书架")
  @DeleteMapping
  public Result<Void> delete(@RequestBody ShelfDeleteDTO dto) {
    shelfService.delete(dto.getShelfId());
    return Result.success();
  }
}
