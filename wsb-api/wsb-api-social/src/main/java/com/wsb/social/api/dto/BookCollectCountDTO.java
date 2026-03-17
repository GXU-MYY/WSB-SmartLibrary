package com.wsb.social.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书籍收藏统计DTO
 */
@Data
public class BookCollectCountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 收藏数
     */
    private Integer collectCount;
}