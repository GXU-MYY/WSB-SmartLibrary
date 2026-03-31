package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("book_id")
    private Long bookId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("borrow_name")
    private String borrowerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("borrowing_time")
    private LocalDate borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_time")
    private LocalDate dueTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("return_time")
    private LocalDate returnTime;

    @JsonProperty("borrow_type")
    private Integer borrowType;

    /**
     * 借阅状态：0-借阅中，1-已归还，2-已逾期
     */
    private Integer status;
}
