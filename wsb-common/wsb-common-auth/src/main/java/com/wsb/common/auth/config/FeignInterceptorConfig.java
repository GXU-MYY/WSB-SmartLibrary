package com.wsb.common.auth.config;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign拦截器，自动传递鉴权Token
 */
@Configuration
public class FeignInterceptorConfig {

  @Bean
  public RequestInterceptor requestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        // 1. 传递用户Token
        if (StpUtil.isLogin()) {
          template.header(StpUtil.getTokenName(), StpUtil.getTokenValue());
        }
        // 2. 传递Same-Token (服务间内部调用鉴权，如果启用了Sa-Token Same-Token)
        // template.header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
      }
    };
  }
}
