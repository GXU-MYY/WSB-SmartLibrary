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
     * 借书记录ID
     */
    private Long id;

    /**
     * 书籍ID
     */
    @JsonProperty("book_id")
    private Long bookId;

    /**
     * 用户ID
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 借书人姓名
     */
    @JsonProperty("borrow_name")
    private String borrowerName;

    /**
     * 借书时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("borrowing_time")
    private LocalDate borrowTime;

    /**
     * 还书时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("return_time")
    private LocalDate returnTime;

    /**
     * 借书类型：1=借入，2=借出
     */
    @JsonProperty("borrow_type")
    private Integer borrowType;

    /**
     * 书籍标题
     */
    private String title;

    /**
     * 书籍封面
     */
    @JsonProperty("pic")
    private String coverUrl;
}