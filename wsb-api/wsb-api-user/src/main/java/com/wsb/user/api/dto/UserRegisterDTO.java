package com.wsb.user.api.dto;

import lombok.Data;

@Data
public class UserRegisterDTO {
  private String phone;
  private String password;
  private String captcha;
}
