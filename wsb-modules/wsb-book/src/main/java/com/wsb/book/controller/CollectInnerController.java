package com.wsb.book.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.dto.BookCollectCountDTO;
import com.wsb.book.api.dto.CollectCategoryStatsDTO;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.Collect;
import com.wsb.book.service.BookService;
import com.wsb.book.service.CollectService;
import com.wsb.book.util.BookKeywordUtils;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏内部接口，供其他服务调用
 */
@RestController
@RequestMapping("/v1/inner/collect")
@RequiredArgsConstructor
public class CollectInnerController {

    private final CollectService collectService;
    private final BookService bookService;

    @GetMapping("/stats/book-count")
    public Result<List<BookCollectCountDTO>> countCollectByBooks(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<Collect> collects;
        if (bookIds != null && !bookIds.isEmpty()) {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .in(Collect::getTargetId, bookIds)
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        } else {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        }

        Map<Long, Long> countMap = collects.stream()
                .collect(Collectors.groupingBy(Collect::getTargetId, Collectors.counting()));

        List<BookCollectCountDTO> result = countMap.entrySet().stream()
                .map(entry -> {
                    BookCollectCountDTO dto = new BookCollectCountDTO();
                    dto.setBookId(entry.getKey());
                    dto.setCollectCount(entry.getValue().intValue());
                    return dto;
                })
                .toList();

        return Result.success(result);
    }

    @GetMapping("/stats/user-collected")
    public Result<Integer> countUserCollected(@RequestParam("user_id") Long userId) {
        long count = collectService.count(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getCollectType, 1)
                .eq(Collect::getIsDeleted, false));
        return Result.success((int) count);
    }

    @GetMapping("/stats/category")
    public Result<List<CollectCategoryStatsDTO>> getCollectStatsByCategory(@RequestParam(value = "book_ids", required = false) List<Long> bookIds) {
        List<Collect> collects;
        if (bookIds != null && !bookIds.isEmpty()) {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .in(Collect::getTargetId, bookIds)
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        } else {
            collects = collectService.list(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
        }

        if (collects.isEmpty()) {
            return Result.success(List.of());
        }

        List<Long> targetBookIds = collects.stream().map(Collect::getTargetId).distinct().toList();
        Map<Long, List<String>> keywordMap = bookService.listByIds(targetBookIds).stream()
                .filter(book -> !Boolean.TRUE.equals(book.getIsDeleted()))
                .collect(Collectors.toMap(Book::getId, book -> BookKeywordUtils.splitKeywords(book.getKeyword()), (a, b) -> a));

        List<CollectCategoryStatsDTO> result = collects.stream()
                .flatMap(collect -> keywordMap.getOrDefault(collect.getTargetId(), List.of()).stream())
                .collect(Collectors.groupingBy(keyword -> keyword, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(java.util.Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(entry -> {
                    CollectCategoryStatsDTO dto = new CollectCategoryStatsDTO();
                    dto.setCategory(entry.getKey());
                    dto.setTotal(entry.getValue().intValue());
                    dto.setCollect(entry.getValue().intValue());
                    return dto;
                })
                .toList();

        return Result.success(result);
    }

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
