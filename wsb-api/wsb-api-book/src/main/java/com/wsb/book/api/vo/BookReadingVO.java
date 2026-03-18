package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 阅读记录VO
 */
@Data
public class BookReadingVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 阅读状态：1-想读，2-在读，3-已读
     */
    private Integer readingStatus;

    /**
     * 书名
     */
    private String bookName;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}