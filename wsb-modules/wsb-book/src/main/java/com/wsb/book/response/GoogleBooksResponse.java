package com.wsb.book.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Google Books API 响应结构
 *
 * @see <a href="https://developers.google.com/books/docs/v1/reference/volumes">Google Books API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String kind;
    private Integer totalItems = 0;
    private List<BookItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private VolumeInfo volumeInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String subtitle;
        private List<String> authors;
        private String publisher;
        private String publishedDate;
        private String description;
        private Integer pageCount;
        private String language;
        private List<String> categories;
        private ImageLinks imageLinks;
        private List<IndustryIdentifier> industryIdentifiers;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String smallThumbnail;
        private String thumbnail;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryIdentifier implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String type;
        private String identifier;
    }
}