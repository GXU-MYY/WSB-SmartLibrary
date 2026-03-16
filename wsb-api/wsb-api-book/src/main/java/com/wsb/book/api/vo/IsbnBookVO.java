package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * ISBN查询结果VO（适配新数据表字段）
 */
@Data
public class IsbnBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String title;
    private String author;
    private String subtitle;
    private String publishDate;
    private String publisher;
    private String pageCount;
    private String price;
    private String summary;
    private String coverUrl;
    private String isbn;
    private String isbn10;
    private String pubplace;
    private String binding;
    private String keyword;
    private String cip;
    private String edition;
    private String impression;
    private String language;
    private String bookFormat;
    private String clc;
}