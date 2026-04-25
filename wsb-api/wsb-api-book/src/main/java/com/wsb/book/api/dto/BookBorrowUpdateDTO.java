package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 借阅记录更新DTO
 */
@Data
public class BookBorrowUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 借阅记录ID
     */
    @NotNull(message = "借阅记录ID不能为空")
    @JsonProperty("borrow_id")
    private Long borrowId;

    /**
     * 借阅对象姓名
     */
    @NotBlank(message = "借阅对象不能为空")
    @JsonProperty("borrow_name")
    private String borrowerName;

    /**
     * 借阅日期
     */
    @JsonProperty("borrowing_time")
    private LocalDate borrowTime;

    /**
     * 预计归还日期
     */
    @JsonProperty("due_time")
    private LocalDate dueTime;
}
