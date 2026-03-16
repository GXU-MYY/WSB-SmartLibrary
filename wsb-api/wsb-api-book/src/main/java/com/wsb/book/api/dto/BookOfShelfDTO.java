package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BookOfShelfDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("book_id")
    private String bookId;

    @JsonProperty("bookshelf_id")
    private String bookshelfId;
}
