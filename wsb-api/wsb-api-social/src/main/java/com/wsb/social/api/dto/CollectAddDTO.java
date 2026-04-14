package com.wsb.social.api.dto;

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

    /**
     * 书架 ID，收藏书架时填写
     */
    @JsonAlias({"bookshelfId", "bookshelf_id"})
    private Long bookshelfId;
}
