package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图书收藏统计 DTO
 */
@Data
public class BookCollectCountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书 ID
     */
    private Long bookId;

    /**
     * 收藏数
     */
    private Integer collectCount;
}
