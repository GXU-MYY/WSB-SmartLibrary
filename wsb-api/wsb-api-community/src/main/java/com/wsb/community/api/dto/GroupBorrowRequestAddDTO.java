package com.wsb.community.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 群组借阅申请 DTO
 */
@Data
public class GroupBorrowRequestAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    private LocalDate dueTime;

    private String requestRemark;
}
