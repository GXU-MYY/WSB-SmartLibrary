package com.wsb.rag.dto;

import lombok.Data;

@Data
public class RecommendedBookPreviewDTO {

    private Long id;

    private String title;

    private String coverUrl;

    private String author;

    private String publisher;

    private String isbn;

    private String summary;

    private String reviewDigest;
}
