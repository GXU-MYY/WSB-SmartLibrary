package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书借阅DTO
 */
@Data
public class BookBorrowDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    @JsonProperty("book_id")
    private Long bookId;

    /**
     * 借阅日期
     */
    @NotNull(message = "借阅日期不能为空")
    @JsonProperty("borrowing_time")
    private LocalDate borrowTime;

    /**
     * 预计归还日期
     */
    @JsonProperty("due_time")
    private LocalDate dueTime;

    /**
     * 借阅对象姓名
     */
    @NotBlank(message = "借阅对象不能为空")
    @JsonProperty("borrow_name")
    private String borrowerName;

    /**
     * 借阅类型：1-借入，2-借出
     */
    @NotNull(message = "借阅类型不能为空")
    @JsonProperty("borrow_type")
    private Integer borrowType;
}
