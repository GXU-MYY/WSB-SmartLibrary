package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书收藏 VO
 */
@Data
public class CollectBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏 ID
     */
    private Long id;

    /**
     * 图书 ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String title;

    /**
     * 图书封面
     */
    private String pic;

    /**
     * 收藏时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;
}
