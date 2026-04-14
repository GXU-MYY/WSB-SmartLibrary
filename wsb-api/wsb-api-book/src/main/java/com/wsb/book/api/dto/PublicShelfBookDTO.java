package com.wsb.book.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 缇ょ粍/绀惧尯鍏紑涔︽灦涓殑鍥句功淇℃伅
 */
@Data
public class PublicShelfBookDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long shelfId;
    private String shelfName;
    private Long ownerUserId;
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private Boolean isBorrowed;
    private Boolean isLentOut;
    private Boolean borrowable;
}
