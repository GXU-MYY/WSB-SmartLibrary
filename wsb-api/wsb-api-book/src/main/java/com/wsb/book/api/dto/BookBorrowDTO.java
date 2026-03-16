package com.wsb.book.api.dto;

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
     * 书籍ID
     */
    @NotBlank(message = "书籍ID不能为空")
    private Long bookId;

    /**
     * 借书时间
     */
    @NotBlank(message = "借书时间不能为空")
    private LocalDate borrowTime;

    /**
     * 借书人姓名
     */
    @NotBlank(message = "借书人姓名不能为空")
    private String borrowerName;

    /**
     * 借书类型：1=借入，2=借出
     */
    @NotNull(message = "借书类型不能为空")
    private Integer borrowType;
}