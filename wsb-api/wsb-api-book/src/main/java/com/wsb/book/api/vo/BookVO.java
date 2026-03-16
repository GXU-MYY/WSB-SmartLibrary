package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书VO
 */
@Data
public class BookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String subtitle;
    private String coverUrl;
    private String author;
    private String summary;
    private String publisher;
    private LocalDate publishDate;
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
    private String clc;
    private String label;
    private String remark;
    private Boolean isOnShelf;
    private Boolean isBorrowed;
    private Long userId;
    private Boolean isDeleted;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}