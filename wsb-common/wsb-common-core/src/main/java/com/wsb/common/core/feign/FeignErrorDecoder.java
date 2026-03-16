package com.wsb.common.core.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Feign 异常解码器
 * 将远程调用的异常解析为 ServiceException
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Exception decode(String methodKey, Response response) {
    try {
      if (response.body() != null) {
        String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
        Result<?> result = objectMapper.readValue(body, Result.class);

        // 如果远程服务返回了错误信息，抛出 ServiceException
        if (result != null) {
          return new ServiceException(result.getCode(), result.getMsg());
        }
      }
    } catch (Exception e) {
      log.error("Feign异常解析失败", e);
    }

    return new ServiceException("系统服务繁忙，请稍后再试");
  }
}
