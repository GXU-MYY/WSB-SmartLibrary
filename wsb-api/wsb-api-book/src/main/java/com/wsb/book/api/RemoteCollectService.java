package com.wsb.book.api;

import com.wsb.book.api.dto.BookCollectCountDTO;
import com.wsb.book.api.dto.CollectCategoryStatsDTO;
import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 收藏远程调用服务
 */
@FeignClient(contextId = "remoteCollectService", value = "wsb-book", path = "/v1/inner")
public interface RemoteCollectService {

    /**
     * 统计图书收藏数
     */
    @GetMapping("/collect/stats/book-count")
    Result<List<BookCollectCountDTO>> countCollectByBooks(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 统计用户收藏的图书数
     */
    @GetMapping("/collect/stats/user-collected")
    Result<Integer> countUserCollected(@RequestParam("user_id") Long userId);

    /**
     * 按分类统计收藏数据
     */
    @GetMapping("/collect/stats/category")
    Result<List<CollectCategoryStatsDTO>> getCollectStatsByCategory(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 获取图书收藏统计总数
     */
    @GetMapping("/collect/stats/summary")
    Result<CollectCategoryStatsDTO> getCollectSummary(@RequestParam("book_ids") List<Long> bookIds);
}
