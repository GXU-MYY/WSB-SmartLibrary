package com.wsb.social.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论删除 DTO
 */
@Data
public class CommentDeleteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论 ID
     */
    @JsonAlias({"commentId", "comment_id"})
    @NotNull(message = "评论ID不能为空")
    private Long commentId;
}
