package com.wsb.social.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏VO
 */
@Data
public class CollectVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 目标ID（书籍ID或书架ID）
     */
    private Long targetId;

    /**
     * 收藏类型：1-图书，2-书架
     */
    private Integer collectType;

    /**
     * 收藏时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;
}