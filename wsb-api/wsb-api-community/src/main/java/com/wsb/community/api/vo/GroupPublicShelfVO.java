package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群组公开书架 VO
 */
@Data
public class GroupPublicShelfVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String shelfName;
    private String remark;
    private Long ownerUserId;
    private String ownerNickname;
}
