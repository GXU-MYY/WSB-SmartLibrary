package com.wsb.social.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评分最高书籍VO
 */
@Data
public class TopRatedBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    private Long id;

    /**
     * 书名
     */
    private String title;

    /**
     * 平均评分
     */
    private Integer stars;

    /**
     * 封面图片
     */
    private String pic;
}