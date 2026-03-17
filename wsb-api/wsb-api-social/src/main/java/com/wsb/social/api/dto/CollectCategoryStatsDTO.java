package com.wsb.social.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏分类统计DTO
 */
@Data
public class CollectCategoryStatsDTO implements Serializable {
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