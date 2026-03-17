package com.wsb.community.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享返回VO
 */
@Data
public class ShareVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享记录ID
     */
    private Long id;

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 目标ID（书籍ID或书架ID）
     */
    private Long targetId;

    /**
     * 分享人用户ID
     */
    private Long sharePerson;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shareTime;
}