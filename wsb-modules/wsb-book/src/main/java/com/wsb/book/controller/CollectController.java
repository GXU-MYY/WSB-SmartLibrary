package com.wsb.book.controller;

import com.wsb.book.api.dto.CollectAddDTO;
import com.wsb.book.api.dto.CollectDeleteDTO;
import com.wsb.book.api.vo.CollectVO;
import com.wsb.book.service.CollectService;
import com.wsb.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏控制器
 */
@Tag(name = "收藏管理")
@RestController
@RequestMapping("/v1/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;

    @Operation(summary = "添加收藏", description = "收藏图书")
    @PostMapping
    public Result<CollectVO> addCollect(@Valid @RequestBody CollectAddDTO dto) {
        return Result.success(collectService.addCollect(dto));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping
    public Result<Void> deleteCollect(@Valid @RequestBody CollectDeleteDTO dto) {
        collectService.deleteCollect(dto);
        return Result.success();
    }

    @Operation(summary = "我的收藏", description = "返回图书收藏")
    @GetMapping
    public Result<?> getMyCollects(
            @Parameter(description = "收藏类型：book-图书收藏")
            @RequestParam(value = "type", required = false, defaultValue = "book") String type) {
        if ("book".equals(type)) {
            return Result.success(collectService.getMyBookCollects());
        }
        return Result.error("type 参数错误，可选值：book");
    }
}
