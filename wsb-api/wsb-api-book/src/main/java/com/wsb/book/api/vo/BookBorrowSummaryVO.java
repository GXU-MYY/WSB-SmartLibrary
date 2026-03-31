package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图书借阅汇总VO
 */
@Data
public class BookBorrowSummaryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer total;

    private Integer borrowedIn;

    private Integer borrowedOut;

    private Integer active;

    private Integer overdue;
}
