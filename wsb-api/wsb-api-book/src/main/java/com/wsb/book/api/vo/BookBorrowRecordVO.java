package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书借阅记录VO
 */
@Data
public class BookBorrowRecordVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 借阅记录ID
     */
    private Long id;

    /**
     * 图书ID
     */
    @JsonProperty("book_id")
    private Long bookId;

    /**
     * 用户ID
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 借阅对象姓名
     */
    @JsonProperty("borrow_name")
    private String borrowerName;

    /**
     * 借阅时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("borrowing_time")
    private LocalDate borrowTime;

    /**
     * 预计归还时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_time")
    private LocalDate dueTime;

    /**
     * 归还时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("return_time")
    private LocalDate returnTime;

    /**
     * 借阅类型：1-借入，2-借出
     */
    @JsonProperty("borrow_type")
    private Integer borrowType;

    /**
     * 借阅状态：0-借阅中，1-已归还，2-已逾期
     */
    private Integer status;

    /**
     * 图书标题
     */
    private String title;

    /**
     * 图书封面
     */
    @JsonProperty("pic")
    private String coverUrl;
}
