package com.wsb.book.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏新增 DTO
 */
@Data
public class CollectAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书 ID，收藏图书时填写
     */
    @JsonAlias({"bookId", "book_id"})
    private Long bookId;
}
