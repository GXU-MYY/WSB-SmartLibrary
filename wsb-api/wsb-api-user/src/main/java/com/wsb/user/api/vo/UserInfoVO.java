package com.wsb.user.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String nickName;
    private String phone;
    private String avatar;
    private String signature;
    private String email;
    private Boolean isActive;
    private Boolean isConfirmed;
    private String idCard;
    private String realName;
    private String rememberToken;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isDeleted;
}