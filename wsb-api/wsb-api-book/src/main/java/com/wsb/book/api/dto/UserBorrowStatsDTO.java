package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户借阅统计DTO
 */
@Data
public class UserBorrowStatsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总借阅数
     */
    private Integer totalBorrowed;

    /**
     * 未归还数
     */
    private Integer unreturned;
}