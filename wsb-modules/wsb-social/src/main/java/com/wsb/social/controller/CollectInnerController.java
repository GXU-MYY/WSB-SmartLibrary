package com.wsb.social.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.common.core.domain.Result;
import com.wsb.social.api.dto.BookCollectCountDTO;
import com.wsb.social.api.dto.CollectCategoryStatsDTO;
import com.wsb.social.domain.Collect;
import com.wsb.social.service.CollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏内部接口（供其他服务调用）
 */
@RestController
@RequestMapping("/v1/inner/collect")
@RequiredArgsConstructor
public class CollectInnerController {

    private final CollectService collectService;

    /**
     * 统计书籍收藏数（批量）
     */
    @GetMapping("/stats/book-count")
    public Result<List<BookCollectCountDTO>> countCollectByBooks(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<Collect> collects;
        if (bookIds != null && !bookIds.isEmpty()) {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .in(Collect::getTargetId, bookIds)
                    .eq(Collect::getCollectType, 1) // 图书
                    .eq(Collect::getIsDeleted, false));
        } else {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        }

        // 按书籍分组统计
        Map<Long, Long> countMap = collects.stream()
                .collect(Collectors.groupingBy(Collect::getTargetId, Collectors.counting()));

        List<BookCollectCountDTO> result = countMap.entrySet().stream()
                .map(e -> {
                    BookCollectCountDTO dto = new BookCollectCountDTO();
                    dto.setBookId(e.getKey());
                    dto.setCollectCount(e.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 统计用户收藏的书籍数
     */
    @GetMapping("/stats/user-collected")
    public Result<Integer> countUserCollected(@RequestParam("user_id") Long userId) {
        long count = collectService.count(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getCollectType, 1) // 图书
                .eq(Collect::getIsDeleted, false));

        return Result.success((int) count);
    }

    /**
     * 按分类统计收藏数据（指定书籍ID列表）
     * 注：由于收藏表没有分类字段，这里简化返回空列表
     */
    @GetMapping("/stats/category")
    public Result<List<CollectCategoryStatsDTO>> getCollectStatsByCategory(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        // 暂时返回空列表，需要关联Book表获取分类信息
        return Result.success(List.of());
    }

    /**
     * 获取书籍收藏统计总数
     */
    @GetMapping("/stats/summary")
    public Result<CollectCategoryStatsDTO> getCollectSummary(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        long total;
        if (bookIds != null && !bookIds.isEmpty()) {
            total = collectService.count(Wrappers.<Collect>lambdaQuery()
                    .in(Collect::getTargetId, bookIds)
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        } else {
            total = collectService.count(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        }

        CollectCategoryStatsDTO dto = new CollectCategoryStatsDTO();
        dto.setTotal((int) total);
        dto.setCollect((int) total);

        return Result.success(dto);
    }
}