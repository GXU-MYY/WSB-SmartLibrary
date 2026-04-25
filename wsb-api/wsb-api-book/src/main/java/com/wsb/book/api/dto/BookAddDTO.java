package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书新增DTO
 */
@Data
public class BookAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String author;
    private String binding;
    private Long shelfId;
    /**
     * 借入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate borrowTime;
    private String cip;
    private String classify;
    /**
     * 中图法分类
     */
    private String clc;
    private String edition;
    private String bookFormat;
    private String impression;
    /**
     * 是否为借来的书
     */
    private Boolean isBorrowed;
    /**
     * 是否在架
     */
    private Boolean isOnShelf;
    private String isbn;
    private String isbn10;
    private String keyword;
    /**
     * 多个标签之间用半角逗号分隔
     */
    private String label;
    private String language;
    /**
     * 所有者（借出方名称）
     */
    private String owner;
    private Integer pageCount;
    private String coverUrl;
    private Double price;
    private LocalDate publishDate;
    private String publisher;
    /**
     * 备注
     */
    private String remark;
    private String subtitle;
    /**
     * 限制在1000中文字符以内
     */
    @Size(max = 1000, message = "简介不能超过1000字")
    private String summary;
    private String title;
}
