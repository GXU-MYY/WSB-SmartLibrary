package com.wsb.common.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.wsb.common.core.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

  /**
   * Sa-Token 未登录异常
   */
  @ExceptionHandler(NotLoginException.class)
  public Result<Void> handleNotLoginException(NotLoginException e) {
    log.error("未登录异常: {}", e.getMessage());
    return Result.error(401, "未登录或Token已过期");
  }

  /**
   * 业务异常
   */
  @ExceptionHandler(ServiceException.class)
  public Result<Void> handleServiceException(ServiceException e) {
    log.warn("业务异常: {}", e.getMessage());
    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * 运行时异常
   */
  @ExceptionHandler(RuntimeException.class)
  public Result<Void> handleRuntimeException(RuntimeException e) {
    log.error("运行时异常: {}", e.getMessage(), e);
    return Result.error(e.getMessage());
  }

  /**
   * 系统异常
   */
  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("系统异常: {}", e.getMessage(), e);
    return Result.error("系统内部错误");
  }
}
