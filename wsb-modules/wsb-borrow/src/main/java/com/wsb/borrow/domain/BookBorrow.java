package com.wsb.borrow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书借阅记录表
 */
@Data
@TableName("t_book_borrow")
public class BookBorrow implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 借阅对方姓名
     */
    private String borrowerName;

    /**
     * 借阅日期
     */
    private LocalDate borrowTime;

    /**
     * 归还日期（NULL表示未归还）
     */
    private LocalDate returnTime;

    /**
     * 借阅方向：1-借入，2-借出
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

    /**
     * 是否删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}