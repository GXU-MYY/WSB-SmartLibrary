package com.wsb.social.api;

import com.wsb.common.core.domain.Result;
import com.wsb.social.api.dto.BookCollectCountDTO;
import com.wsb.social.api.dto.CollectCategoryStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 收藏远程调用服务
 */
@FeignClient(value = "wsb-social", path = "/v1/inner")
public interface RemoteCollectService {

    /**
     * 统计书籍收藏数（批量）
     */
    @GetMapping("/collect/stats/book-count")
    Result<List<BookCollectCountDTO>> countCollectByBooks(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 统计用户收藏的书籍数
     */
    @GetMapping("/collect/stats/user-collected")
    Result<Integer> countUserCollected(@RequestParam("user_id") Long userId);

    /**
     * 按分类统计收藏数据（指定书籍ID列表）
     */
    @GetMapping("/collect/stats/category")
    Result<List<CollectCategoryStatsDTO>> getCollectStatsByCategory(@RequestParam("book_ids") List<Long> bookIds);

    /**
     * 获取书籍收藏统计总数
     */
    @GetMapping("/collect/stats/summary")
    Result<CollectCategoryStatsDTO> getCollectSummary(@RequestParam("book_ids") List<Long> bookIds);
}