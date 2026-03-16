package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书架新增DTO
 */
@Data
public class ShelfAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书架名称
     */
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
     * 类型：1-实体书架，2-虚拟书单
     */
    private Integer shelfType;
}