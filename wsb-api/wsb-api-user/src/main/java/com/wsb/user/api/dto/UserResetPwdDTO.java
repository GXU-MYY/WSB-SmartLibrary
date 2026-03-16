package com.wsb.user.api.dto;

import lombok.Data;

@Data
public class UserResetPwdDTO {
  private String phone;
  private String passwd;
  private String captcha;
}
