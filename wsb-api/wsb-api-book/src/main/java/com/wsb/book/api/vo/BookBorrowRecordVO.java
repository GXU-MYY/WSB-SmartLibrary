package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Long bookId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 借书人姓名
     */
    private String borrowerName;

    /**
     * 借书时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate borrowTime;

    /**
     * 还书时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate returnTime;

    /**
     * 借书类型：1=借入，2=借出
     */
    private Integer borrowType;

    /**
     * 书籍标题
     */
    private String title;

    /**
     * 书籍封面
     */
    private String coverUrl;
}