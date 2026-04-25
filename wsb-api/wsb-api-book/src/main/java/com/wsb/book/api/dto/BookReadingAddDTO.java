package com.wsb.book.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 阅读记录新增DTO
 */
@Data
public class BookReadingAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 阅读状态：1-想读，2-在读，3-已读
     */
    @NotNull(message = "阅读状态不能为空")
    private Integer readingStatus;
}