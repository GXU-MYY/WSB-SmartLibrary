package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群组公开图书 VO
 */
@Data
public class GroupPublicBookVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long shelfId;
    private String shelfName;
    private Long ownerUserId;
    private String ownerNickname;
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private Boolean isBorrowed;
    private Boolean isLentOut;
    private Boolean borrowable;
}
