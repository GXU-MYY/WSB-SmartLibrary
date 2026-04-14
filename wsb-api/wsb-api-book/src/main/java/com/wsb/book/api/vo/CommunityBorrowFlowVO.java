package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 绀剧兢鍊熼槄娴佺▼鍒涘缓缁撴灉
 */
@Data
public class CommunityBorrowFlowVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String borrowFlowId;
    private Long inBorrowId;
    private Long outBorrowId;
}
