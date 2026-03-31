package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 还书DTO
 */
@Data
public class BookReturnDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 借阅记录ID
     */
    @NotNull(message = "借阅记录ID不能为空")
    @JsonProperty("borrow_id")
    private Long borrowId;

    /**
     * 归还时间
     */
    @NotNull(message = "归还时间不能为空")
    @JsonProperty("return_time")
    private LocalDate returnTime;
}
