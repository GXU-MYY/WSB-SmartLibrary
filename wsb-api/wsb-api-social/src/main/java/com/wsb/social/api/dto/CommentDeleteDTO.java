package com.wsb.social.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论删除DTO
 */
@Data
public class CommentDeleteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @NotNull(message = "评论ID不能为空")
    private Long commentId;
}