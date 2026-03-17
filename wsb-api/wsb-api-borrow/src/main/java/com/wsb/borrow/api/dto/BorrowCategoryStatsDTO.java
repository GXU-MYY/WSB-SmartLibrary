package com.wsb.borrow.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 借阅分类统计DTO
 */
@Data
public class BorrowCategoryStatsDTO implements Serializable {
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
     * 阅读中
     */
    private Integer reading;

    /**
     * 已读
     */
    private Integer read;
}