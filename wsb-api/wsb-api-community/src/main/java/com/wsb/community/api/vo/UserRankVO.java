package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户排名VO
 */
@Data
public class UserRankVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 书籍数量
     */
    private Integer bookCount;

    /**
     * 排名
     */
    private Integer ranking;
}