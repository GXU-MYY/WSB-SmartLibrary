package com.wsb.social.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏新增DTO
 */
@Data
public class CollectAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书ID（收藏图书时必填）
     */
    private Long bookId;

    /**
     * 书架ID（收藏书架时必填）
     */
    private Long bookshelfId;
}