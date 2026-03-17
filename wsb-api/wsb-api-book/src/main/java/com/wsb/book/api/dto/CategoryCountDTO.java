package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类统计DTO
 */
@Data
public class CategoryCountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String category;

    /**
     * 数量
     */
    private Integer count;
}