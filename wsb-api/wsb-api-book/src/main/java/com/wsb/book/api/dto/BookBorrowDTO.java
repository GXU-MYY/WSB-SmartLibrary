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
     * 图书ID。借出时必填；线下借入时由后端创建图书后回填。
     */
    @JsonProperty("book_id")
    private Long bookId;

    /**
     * 线下借入时可选放入的书架ID。
     */
    @JsonProperty("shelf_id")
    private Long shelfId;

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
     * 借阅对象姓名。借出时表示借阅人；借入时表示出借人。
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

    /**
     * 线下借入图书元数据
     */
    private String title;
    private String subtitle;
    private String author;
    private String publisher;

    @JsonProperty("publish_date")
    private LocalDate publishDate;

    @JsonProperty("page_count")
    private Integer pageCount;

    private Double price;
    private String binding;
    private String isbn;
    private String isbn10;
    private String keyword;

    @JsonProperty("cover_url")
    private String coverUrl;
}
