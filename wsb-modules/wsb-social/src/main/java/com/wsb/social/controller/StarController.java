package com.wsb.social.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.social.api.vo.TopRatedBookVO;
import com.wsb.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分控制器
 */
@Tag(name = "评分管理")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class StarController {

    private final CommentService commentService;

    @Operation(summary = "评分最高", description = "返回评分最高的书籍列表")
    @GetMapping("/stars")
    public Result<List<TopRatedBookVO>> getTopRated(
            @Parameter(description = "返回前N条记录，默认5条")
            @RequestParam(value = "top", required = false) Integer top) {
        return Result.success(commentService.getTopRatedBooks(top));
    }
}