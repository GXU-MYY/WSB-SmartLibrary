package com.wsb.social.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 书籍评论列表VO
 */
@Data
public class BookCommentListVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 平均评分
     */
    private Integer starMean;

    /**
     * 评论列表
     */
    private List<CommentVO> comments;
}