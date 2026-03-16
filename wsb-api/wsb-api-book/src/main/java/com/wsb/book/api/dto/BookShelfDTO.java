package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图书上架DTO（原 BookOfShelfDTO）
 */
@Data
public class BookShelfDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("book_id")
    private Long bookId;

    @JsonProperty("shelf_id")
    private Long shelfId;
}