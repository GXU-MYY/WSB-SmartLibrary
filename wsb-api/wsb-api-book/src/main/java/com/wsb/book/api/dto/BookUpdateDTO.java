package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书更新DTO
 */
@Data
public class BookUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "图书ID不能为空")
    private Long id;

    private String title;
    private String subtitle;
    private String coverUrl;
    private String author;

    @Size(max = 1000, message = "简介不能超过1000字")
    private String summary;

    private String publisher;

    @JsonFormat(pattern = "yyyy-MM-dd")
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
}
