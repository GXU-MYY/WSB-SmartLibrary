package com.wsb.borrow.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 书籍借阅统计DTO
 */
@Data
public class BookBorrowCountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 借阅次数
     */
    private Integer borrowCount;
}