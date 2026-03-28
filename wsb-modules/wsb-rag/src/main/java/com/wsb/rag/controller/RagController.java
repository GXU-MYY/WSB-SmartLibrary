package com.wsb.rag.controller;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
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

/**
 * RAG 接口控制器
 */
@Tag(name = "智能推荐")
@Slf4j
@RestController
@RequestMapping("/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final BookAiContentService bookAiContentService;
    private final RemoteBookService remoteBookService;

    @Operation(summary = "智能推荐", description = "自然语言查询推荐书籍")
    @PostMapping("/recommend")
    public Result<List<BookRemoteDTO>> recommend(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(ragService.recommend(query, limit));
    }

    @Operation(summary = "相似图书", description = "获取与指定书籍相似的其他书籍")
    @GetMapping("/similar/{bookId}")
    public Result<List<BookRemoteDTO>> getSimilar(
            @PathVariable Long bookId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(ragService.getSimilarBooks(bookId, limit));
    }

    @Operation(summary = "生成AI摘要", description = "为指定书籍生成 AI 摘要")
    @PostMapping("/summary/{bookId}")
    public Result<String> generateSummary(@PathVariable Long bookId) {
        return Result.success(bookAiContentService.generateSummary(bookId));
    }

    @Operation(summary = "获取AI摘要", description = "获取指定书籍的 AI 摘要")
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
        log.info("收到聚合网络书评请求: bookId={}", bookId);
        var result = remoteBookService.getBookById(bookId);
        if (result.getData() == null) {
            return Result.success("书籍不存在");
        }

        BookRemoteDTO book = result.getData();
        String reviews = bookAiContentService.aggregateReviews(bookId, book.getTitle(), book.getAuthor());
        log.info("返回聚合网络书评结果: bookId={}", bookId);
        return Result.success(reviews);
    }

    @Operation(summary = "获取聚合网络书评", description = "优先返回 Redis 中已缓存的聚合书评")
    @GetMapping("/reviews/{bookId}")
    public Result<String> getAggregatedReviews(@PathVariable Long bookId) {
        log.info("读取聚合网络书评缓存: bookId={}", bookId);
        return Result.success(bookAiContentService.getCachedReviewDigest(bookId));
    }
}
