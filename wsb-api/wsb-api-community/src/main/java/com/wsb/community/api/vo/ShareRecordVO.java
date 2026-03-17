package com.wsb.community.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享记录VO
 */
@Data
public class ShareRecordVO implements Serializable {
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
     * 分享类型：book-书籍，bookshelf-书架
     */
    private String shareType;

    /**
     * 分享人用户ID
     */
    private Long sharePerson;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shareTime;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 书籍/书架名称
     */
    private String name;
}