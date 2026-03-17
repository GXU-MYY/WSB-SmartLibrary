package com.wsb.borrow.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 还书DTO
 */
@Data
public class BookReturnDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    @NotBlank(message = "书籍ID不能为空")
    @JsonProperty("book_id")
    private String bookId;

    /**
     * 借书类型：1=借入，2=借出
     */
    @NotBlank(message = "借书类型不能为空")
    @JsonProperty("borrow_type")
    private String borrowType;

    /**
     * 还书时间
     */
    @NotBlank(message = "还书时间不能为空")
    @JsonProperty("return_time")
    private String returnTime;
}