package com.wsb.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickName;

    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 255, message = "个性签名长度不能超过255个字符")
    private String signature;

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatar;
}
