package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 绀剧兢鍊熼槄娴佺▼鍒涘缓DTO锛堜緵鍐呴儴鏈嶅姟璋冪敤锛?
 */
@Data
public class CommunityBorrowCreateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long groupId;
    private Long requestId;
    private Long bookId;
    private Long ownerUserId;
    private String ownerNickname;
    private Long borrowerUserId;
    private String borrowerNickname;
    private LocalDate borrowTime;
    private LocalDate dueTime;
}
