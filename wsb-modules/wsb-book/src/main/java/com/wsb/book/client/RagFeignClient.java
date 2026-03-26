package com.wsb.book.client;

import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "wsb-rag", path = "/v1/inner/rag")
public interface RagFeignClient {

    @PostMapping("/summary/{bookId}/enqueue")
    Result<Void> enqueueSummary(@PathVariable("bookId") Long bookId);
}
