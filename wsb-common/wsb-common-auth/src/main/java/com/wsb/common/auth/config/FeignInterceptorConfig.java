package com.wsb.common.auth.config;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 拦截器，自动透传当前用户 token。
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignInterceptorConfig {

  @Bean
  public RequestInterceptor requestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        try {
          if (StpUtil.isLogin()) {
            template.header(StpUtil.getTokenName(), StpUtil.getTokenValue());
          }
        } catch (NotWebContextException ignored) {
          // MQ consumer and scheduled job threads do not have HttpServletRequest.
        }
      }
    };
  }
}
