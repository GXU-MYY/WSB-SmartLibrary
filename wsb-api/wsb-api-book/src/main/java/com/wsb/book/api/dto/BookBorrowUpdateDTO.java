package com.wsb.book.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 借书记录更新DTO
 */
@Data
public class BookBorrowUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 借书记录ID
     */
    @NotNull(message = "借书记录ID不能为空")
    private Long borrowId;

    /**
     * 借书人姓名
     */
    @NotBlank(message = "借书人姓名不能为空")
    private String borrowerName;

    /**
     * 借书时间
     */
    private LocalDate borrowTime;
}