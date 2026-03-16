package com.wsb.book.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 阿里云 ISBN API 响应结构
 *
 * @see <a href="https://market.aliyun.com/products/57126001/cmapi00032560.html">阿里云ISBN图书查询API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliyunIsbnResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private String taskNo;
    private DataWrapper data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<BookDetail> details;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookDetail implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String series;
        private String title;
        private String author;
        private String publisher;
        private String pubDate;
        private String pubPlace;
        private String isbn;
        private String isbn10;
        private String price;
        private String genus;
        private String levelNum;
        private String heatNum;
        private String format;
        private String binding;
        private String page;
        private String wordNum;
        private String edition;
        private String yinci;
        private String paper;
        private String language;
        private String keyword;
        private String img;
        private String bookCatalog;
        private String gist;
        private String cipTxt;
        private String annotation;
        private String subject;
        private String batch;
    }
}