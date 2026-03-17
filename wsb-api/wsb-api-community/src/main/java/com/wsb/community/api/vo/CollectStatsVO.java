package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 收藏统计VO
 */
@Data
public class CollectStatsVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 收藏数
     */
    private Integer collect;

    /**
     * 分类统计列表
     */
    private List<CategoryCollectVO> classifyList;

    @Data
    public static class CategoryCollectVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 分类名称
         */
        private String category;

        /**
         * 总数
         */
        private Integer total;

        /**
         * 收藏数
         */
        private Integer collect;
    }
}