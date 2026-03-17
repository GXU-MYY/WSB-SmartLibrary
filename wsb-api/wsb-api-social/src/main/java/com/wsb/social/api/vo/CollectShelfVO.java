package com.wsb.social.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书架收藏VO
 */
@Data
public class CollectShelfVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 书架ID
     */
    private Long shelfId;

    /**
     * 书架名称
     */
    private String shelfName;

    /**
     * 收藏时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;
}