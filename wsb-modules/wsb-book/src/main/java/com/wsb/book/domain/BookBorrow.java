package com.wsb.book.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 图书借阅记录表（用于书籍模块内部操作）
 */
@Data
@TableName("t_book_borrow")
public class BookBorrow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("book_id")
    private Long bookId;

    @TableField("user_id")
    private Long userId;

    private String borrowerName;

    private LocalDate borrowTime;

    private LocalDate returnTime;

    private Integer borrowType;

    private String bookName;

    private String coverUrl;

    private Boolean isDeleted;
}