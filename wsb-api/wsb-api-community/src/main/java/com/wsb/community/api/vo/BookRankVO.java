package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书籍排名VO
 */
@Data
public class BookRankVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 书籍标题
     */
    private String title;

    /**
     * 封面URL
     */
    private String pic;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 排名
     */
    private Integer ranking;
}