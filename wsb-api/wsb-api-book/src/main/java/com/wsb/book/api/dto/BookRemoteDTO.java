package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书籍远程调用DTO
 */
@Data
public class BookRemoteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String coverUrl;
}