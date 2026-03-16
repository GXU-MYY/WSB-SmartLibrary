package com.wsb.common.core.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
public class Result<T> implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private int code;
  private String msg;
  private T data;

  public static <T> Result<T> success() {
    return success(null);
  }

  public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setMsg("操作成功");
    result.setData(data);
    return result;
  }

  public static <T> Result<T> error() {
    return error(500, "操作失败");
  }

  public static <T> Result<T> error(String msg) {
    return error(500, msg);
  }

  public static <T> Result<T> error(int code, String msg) {
    Result<T> result = new Result<>();
    result.setCode(code);
    result.setMsg(msg);
    return result;
  }
}
