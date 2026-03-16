package com.wsb.book.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BookReturnDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    @NotBlank(message = "书籍ID不能为空")
    private String bookId;

    /**
     * 借书类型：1=借入，2=借出
     */
    @NotBlank(message = "借书类型不能为空")
    private String borrowType;

    /**
     * 还书时间
     */
    @NotBlank(message = "还书时间不能为空")
    private String returnTime;
}
