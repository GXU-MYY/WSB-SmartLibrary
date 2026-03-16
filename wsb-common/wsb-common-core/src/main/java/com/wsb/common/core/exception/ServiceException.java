package com.wsb.common.core.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class ServiceException extends RuntimeException {
  private Integer code;

  public ServiceException(String message) {
    this(500, message);
  }

  public ServiceException(Integer code, String message) {
    super(message);
    this.code = code;
  }
}
