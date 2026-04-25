package com.wsb.rag.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/inner/rag")
@RequiredArgsConstructor
public class RagInnerController {

    private final RagService ragService;

    @PostMapping("/summary/{bookId}/enqueue")
    public Result<Void> enqueueSummary(@PathVariable Long bookId) {
        ragService.enqueueSummary(bookId);
        return Result.success();
    }

    @PostMapping("/embedding/{bookId}/enqueue")
    public Result<Void> enqueueEmbedding(@PathVariable Long bookId) {
        ragService.enqueueEmbedding(bookId);
        return Result.success();
    }

    @PostMapping("/dlq/{taskType}/requeue")
    public Result<Integer> requeueDeadLetters(
            @PathVariable String taskType,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.success(ragService.requeueDeadLetters(taskType, limit));
    }
}
