package com.wsb.book.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书架/书单表
 */
@Data
@TableName("t_shelf")
public class Shelf implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 书架/书单名称
     */
    @TableField("shelf_name")
    private String shelfName;

    /**
     * 书架物理位置
     */
    private String address;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 书架描述
     */
    private String remark;

    /**
     * 所有者用户ID
     */
    private Long userId;

    /**
     * 类型：1-实体书架，2-虚拟书单
     */
    private Integer shelfType;

    /**
     * 是否删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}