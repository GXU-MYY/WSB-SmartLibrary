package com.wsb.community.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分享请求DTO
 */
@Data
public class ShareAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 书籍ID（分享书籍时必填）
     */
    private Long bookId;

    /**
     * 书架ID（分享书架时必填）
     */
    private Long bookshelfId;
}