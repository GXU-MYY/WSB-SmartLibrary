package com.wsb.social.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论VO
 */
@Data
public class CommentVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 评论时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime comTime;

    /**
     * 评分
     */
    private Integer stars;
}