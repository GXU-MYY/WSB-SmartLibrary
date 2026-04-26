package com.wsb.rag.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.dto.RecommendedBookPreviewDTO;
import com.wsb.rag.service.BookAiContentService;
import com.wsb.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能推荐")
@Slf4j
@RestController
@RequestMapping("/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;
    private static final String DEFAULT_SIMILAR_LIMIT = "4";

    private final RagService ragService;
    private final BookAiContentService bookAiContentService;
    private final RemoteBookService remoteBookService;

    @Operation(summary = "智能推荐", description = "自然语言查询推荐图书")
    @GetMapping("/recommend")
    public Result<List<BookRemoteDTO>> recommend(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(value = "mineOnly", defaultValue = "false") Boolean mineOnly) {
        if (!isValidLimit(limit)) {
            return Result.error(400, buildLimitErrorMessage());
        }
        if (Boolean.TRUE.equals(mineOnly) && !StpUtil.isLogin()) {
            return Result.error(401, "请先登录后再查看我的推荐");
        }
        Long ownerId = Boolean.TRUE.equals(mineOnly) ? StpUtil.getLoginIdAsLong() : null;
        return Result.success(ragService.recommend(query, limit, ownerId));
    }

    @Operation(summary = "推荐图书预览", description = "推荐入口使用的受限图书详情")
    @GetMapping("/books/{bookId}/preview")
    public Result<RecommendedBookPreviewDTO> getRecommendedBookPreview(@PathVariable Long bookId) {
        return Result.success(ragService.getRecommendedBookPreview(bookId));
    }

    @Operation(summary = "相似图书", description = "获取与指定图书相似的其他图书")
    @GetMapping("/similar/{bookId}")
    public Result<List<BookRemoteDTO>> getSimilar(
            @PathVariable Long bookId,
            @RequestParam(value = "limit", defaultValue = DEFAULT_SIMILAR_LIMIT) Integer limit) {
        if (!isValidLimit(limit)) {
            return Result.error(400, buildLimitErrorMessage());
        }
        return Result.success(ragService.getSimilarBooks(bookId, limit));
    }

    @Operation(summary = "生成 AI 摘要", description = "为指定图书生成 AI 摘要")
    @PostMapping("/summary/{bookId}")
    public Result<String> generateSummary(@PathVariable Long bookId) {
        return Result.success(bookAiContentService.generateSummary(bookId));
    }

    @Operation(summary = "获取 AI 摘要", description = "获取指定图书的 AI 摘要")
    @GetMapping("/summary/{bookId}")
    public Result<String> getSummary(@PathVariable Long bookId) {
        var result = remoteBookService.getBookById(bookId);
        if (result.getData() == null) {
            return Result.success(null);
        }
        return Result.success(result.getData().getSummary());
    }

    @Operation(summary = "聚合网络书评", description = "搜索并聚合网络书评")
    @PostMapping("/reviews/{bookId}")
    public Result<String> aggregateReviews(@PathVariable Long bookId) {
        log.info("received review aggregation request: bookId={}", bookId);
        var result = remoteBookService.getBookById(bookId);
        if (result.getData() == null) {
            return Result.success("图书不存在");
        }

        BookRemoteDTO book = result.getData();
        String reviews = bookAiContentService.aggregateReviews(bookId, book.getTitle(), book.getAuthor());
        log.info("review aggregation completed: bookId={}", bookId);
        return Result.success(reviews);
    }

    @Operation(summary = "获取聚合书评", description = "优先返回已缓存的聚合书评")
    @GetMapping("/reviews/{bookId}")
    public Result<String> getAggregatedReviews(@PathVariable Long bookId) {
        log.info("read cached review digest: bookId={}", bookId);
        return Result.success(bookAiContentService.getCachedReviewDigest(bookId));
    }

    private boolean isValidLimit(Integer limit) {
        return limit != null && limit >= MIN_LIMIT && limit <= MAX_LIMIT;
    }

    private String buildLimitErrorMessage() {
        return "limit 参数错误，可选范围：" + MIN_LIMIT + "-" + MAX_LIMIT;
    }
}
