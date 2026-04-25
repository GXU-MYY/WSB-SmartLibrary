package com.wsb.community.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 群组借阅申请 VO
 */
@Data
public class GroupBorrowRequestVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long bookId;
    private String bookName;
    private String coverUrl;
    private Long shelfId;
    private String shelfName;
    private Long ownerUserId;
    private String ownerNickname;
    private Long borrowerUserId;
    private String borrowerNickname;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueTime;
    private String requestRemark;
    private String borrowFlowId;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
