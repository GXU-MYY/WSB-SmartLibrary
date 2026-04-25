package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群组成员VO
 */
@Data
public class GroupUserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成员记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;
}
