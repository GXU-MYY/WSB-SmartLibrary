package com.wsb.book.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 我的书籍列表VO
 */
@Data
public class MyBookListVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 书籍总数
     */
    private Integer count;

    /**
     * 书籍列表
     */
    private List<MyBookVO> books;
}