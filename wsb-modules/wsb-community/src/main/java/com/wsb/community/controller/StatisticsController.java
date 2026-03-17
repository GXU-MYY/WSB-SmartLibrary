package com.wsb.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.common.core.domain.Result;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.BorrowStatsVO;
import com.wsb.community.api.vo.CollectStatsVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;
import com.wsb.community.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统计控制器
 */
@Tag(name = "统计分析")
@RestController
@RequestMapping("/v1/community/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "排名查询", description = "type=book-书籍收藏排名，type=user-用户拥书排名")
    @GetMapping("/rank")
    public Result<?> getRank(
            @Parameter(description = "类型：book-书籍排名，user-用户排名")
            @RequestParam("type") String type,
            @Parameter(description = "页码")
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页数量")
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize) {
        if ("book".equals(type)) {
            return Result.success(statisticsService.getBookRank(page, pageSize));
        } else if ("user".equals(type)) {
            return Result.success(statisticsService.getUserRank(page, pageSize));
        } else {
            return Result.error("type参数错误，可选值：book、user");
        }
    }

    @Operation(summary = "统计汇总", description = "scope=all-全站统计，scope=mine-我的统计；type=borrow-借阅统计，type=collect-收藏统计")
    @GetMapping("/summary")
    public Result<?> getSummary(
            @Parameter(description = "范围：all-全站，mine-我的")
            @RequestParam("scope") String scope,
            @Parameter(description = "类型：borrow-借阅统计，collect-收藏统计")
            @RequestParam("type") String type) {
        if (!"all".equals(scope) && !"mine".equals(scope)) {
            return Result.error("scope参数错误，可选值：all、mine");
        }

        if ("borrow".equals(type)) {
            return Result.success(statisticsService.getBorrowStats(scope));
        } else if ("collect".equals(type)) {
            return Result.success(statisticsService.getCollectStats(scope));
        } else {
            return Result.error("type参数错误，可选值：borrow、collect");
        }
    }

    @Operation(summary = "个人分析", description = "当前用户的完整统计分析")
    @GetMapping("/personal")
    public Result<PersonalStatsVO> getPersonalStats() {
        return Result.success(statisticsService.getPersonalStats());
    }
}