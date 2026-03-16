package com.wsb.user.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String nickName;
    private String signature;
    private String avatar;
}