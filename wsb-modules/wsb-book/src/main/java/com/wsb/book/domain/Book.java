package com.wsb.book.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书信息表
 */
@Data
@TableName("t_book")
public class Book implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 书名（主标题）
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 作者
     */
    private String author;

    /**
     * 内容简介
     */
    private String summary;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private LocalDate publishDate;

    /**
     * 页数
     */
    private Integer pageCount;

    /**
     * 定价
     */
    private Double price;

    /**
     * 装帧方式
     */
    private String binding;

    /**
     * ISBN-13
     */
    private String isbn;

    /**
     * ISBN-10
     */
    private String isbn10;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 版次
     */
    private String edition;

    /**
     * 印次
     */
    private String impression;

    /**
     * 语言
     */
    private String language;

    /**
     * 开本
     */
    private String bookFormat;

    /**
     * 用户自定义分类
     */
    private String classify;

    /**
     * CIP核字号
     */
    private String cip;

    /**
     * 中图法分类
     */
    private String clc;

    /**
     * 书籍标签
     */
    private String label;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 是否在架
     */
    private Boolean isOnShelf;

    /**
     * 是否为借来的书
     */
    private Boolean isBorrowed;

    /**
     * 所有者用户ID
     */
    private Long userId;

    /**
     * 是否删除
     */
    private Boolean isDeleted;

    /**
     * 向量状态：0-未处理 1-处理中 2-已完成
     */
    private Integer embeddingStatus;

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