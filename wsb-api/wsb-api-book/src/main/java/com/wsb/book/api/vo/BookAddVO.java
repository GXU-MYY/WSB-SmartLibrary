package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图书添加返回VO
 */
@Data
public class BookAddVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String coverUrl;
}