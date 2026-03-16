package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书架删除DTO
 */
@Data
public class ShelfDeleteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书架ID
     */
    @JsonProperty("shelf_id")
    private Long shelfId;
}