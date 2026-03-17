package com.wsb.social.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书籍收藏VO
 */
@Data
public class CollectBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 书籍标题
     */
    private String title;

    /**
     * 书籍封面
     */
    private String pic;

    /**
     * 收藏时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;
}