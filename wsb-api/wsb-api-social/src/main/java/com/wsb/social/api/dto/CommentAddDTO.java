package com.wsb.social.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论新增 DTO
 */
@Data
public class CommentAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书 ID
     */
    @JsonAlias({"bookId", "book_id"})
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String comment;

    /**
     * 评分，1 到 5 星
     */
    @JsonAlias({"starRating", "star_rating"})
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1星")
    @Max(value = 5, message = "评分最大为5星")
    private Integer starRating;
}
