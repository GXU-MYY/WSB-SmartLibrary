package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我的书籍VO（用于列表返回，适配新数据表字段）
 */
@Data
public class MyBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String subtitle;
    private String coverUrl;
    private String author;
    private String summary;
    private String publisher;
    private String publishDate;
    private Integer pageCount;
    private Double price;
    private String binding;
    private String isbn;
    private String isbn10;
    private String keyword;
    private String edition;
    private String impression;
    private String language;
    private String bookFormat;
    private String classify;
    private String cip;
    private Boolean isOnShelf;
    private Boolean isBorrowed;
    private Long userId;
    private String clc;
    private String label;
    private String remark;
}