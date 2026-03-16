package com.wsb.user.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户认证信息传输对象 (包含密码等敏感信息，仅限内部调用)
 */
@Data
public class UserRemoteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String password; // 加密后的密码
    private String phone;
    private Boolean isActive; // 是否激活
    private Boolean isConfirmed; // 是否确认
}