package com.wsb.common.core.feign;

import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(feign.Feign.class)
public class FeignConfig {

  @Bean
  public ErrorDecoder errorDecoder() {
    return new FeignErrorDecoder();
  }
}
