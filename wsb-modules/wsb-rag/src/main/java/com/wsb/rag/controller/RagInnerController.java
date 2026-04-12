package com.wsb.rag.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
