package com.wsb.community.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群组VO
 */
@Data
public class GroupVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群组ID
     */
    private Long id;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 群主用户ID
     */
    private Long ownerId;

    /**
     * 群组描述
     */
    private String remark;

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