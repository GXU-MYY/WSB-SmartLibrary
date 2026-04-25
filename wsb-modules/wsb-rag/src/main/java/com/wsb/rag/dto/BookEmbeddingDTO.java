package com.wsb.rag.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书籍嵌入 DTO（用于消息队列）
 */
@Data
public class BookEmbeddingDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long bookId;
    private String title;
    private String author;
    private String summary;
    private String keyword;
    private String label;
}