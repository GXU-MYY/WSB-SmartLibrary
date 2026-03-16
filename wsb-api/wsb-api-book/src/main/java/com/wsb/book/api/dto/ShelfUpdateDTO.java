package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书架更新DTO
 */
@Data
public class ShelfUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String shelfName;
    private Boolean isPublic;
    private String address;
    private String remark;
}