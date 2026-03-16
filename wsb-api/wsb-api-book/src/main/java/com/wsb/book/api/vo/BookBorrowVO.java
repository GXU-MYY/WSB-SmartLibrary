package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书借阅VO
 */
@Data
public class BookBorrowVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long bookId;
    private Long userId;
    private String borrowerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate returnTime;

    /**
     * 借书类型：1=借入，2=借出
     */
    private Integer borrowType;

    /**
     * 图书名称（冗余）
     */
    private String bookName;

    /**
     * 封面URL（冗余）
     */
    private String coverUrl;
}