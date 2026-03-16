package com.wsb.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享表
 */
@Data
@TableName("t_share")
public class Share implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 目标群组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 分享目标ID（book_id或shelf_id）
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 分享类型：1-图书，2-书架
     */
    @TableField("share_type")
    private Integer shareType;

    /**
     * 分享人用户ID
     */
    @TableField("share_user_id")
    private Long shareUserId;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Boolean isDeleted;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}