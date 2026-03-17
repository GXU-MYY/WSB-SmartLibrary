package com.wsb.social.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.social.api.dto.CollectAddDTO;
import com.wsb.social.api.dto.CollectDeleteDTO;
import com.wsb.social.api.vo.CollectBookVO;
import com.wsb.social.api.vo.CollectShelfVO;
import com.wsb.social.api.vo.CollectVO;
import com.wsb.social.service.CollectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 */
@Tag(name = "收藏管理")
@RestController
@RequestMapping("/v1/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;

    @Operation(summary = "添加收藏", description = "收藏书籍或书架，bookId和bookshelfId二选一")
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

    @Operation(summary = "我的收藏", description = "type=book返回书籍收藏，type=bookshelf返回书架收藏")
    @GetMapping
    public Result<?> getMyCollects(
            @Parameter(description = "收藏类型：book-书籍收藏，bookshelf-书架收藏")
            @RequestParam(value = "type") String type) {
        if ("book".equals(type)) {
            return Result.success(collectService.getMyBookCollects());
        } else if ("bookshelf".equals(type)) {
            return Result.success(collectService.getMyShelfCollects());
        } else {
            return Result.error("type参数错误，可选值：book、bookshelf");
        }
    }
}