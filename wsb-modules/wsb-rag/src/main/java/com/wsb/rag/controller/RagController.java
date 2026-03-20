package com.wsb.rag.controller;

import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.RagService;
import com.wsb.rag.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 接口控制器
 */
@Tag(name = "智能推荐")
@RestController
@RequestMapping("/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final SummaryService summaryService;
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

    @Operation(summary = "生成AI摘要", description = "为指定书籍生成AI摘要")
    @PostMapping("/summary/{bookId}")
    public Result<String> generateSummary(@PathVariable Long bookId) {
        String summary = summaryService.generateSummary(bookId);
        return Result.success(summary);
    }

    @Operation(summary = "获取AI摘要", description = "获取指定书籍的摘要")
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
        var result = remoteBookService.getBookById(bookId);
        if (result.getData() == null) {
            return Result.success("书籍不存在");
        }
        BookRemoteDTO book = result.getData();
        String reviews = summaryService.aggregateReviews(bookId, book.getTitle(), book.getAuthor());
        return Result.success(reviews);
    }
}