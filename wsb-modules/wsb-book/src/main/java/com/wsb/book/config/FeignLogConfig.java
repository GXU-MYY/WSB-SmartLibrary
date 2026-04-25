package com.wsb.book.config;

import feign.Logger;
import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Feign 日志配置
 * 用于调试 HTTP 请求
 */
@Slf4j
@Configuration
public class FeignLogConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public feign.Logger feignLogger() {
        return new feign.Logger() {
            @Override
            protected void log(String configKey, String format, Object... args) {
                log.info("[Feign] {} - {}", configKey, String.format(format, args));
            }

            @Override
            protected void logRequest(String configKey, Level logLevel, Request request) {
                log.info("========== Feign Request ==========");
                log.info("ConfigKey: {}", configKey);
                log.info("Method: {}", request.httpMethod());
                log.info("URL: {}", request.url());
                log.info("Headers: {}", request.headers());

                if (request.body() != null) {
                    log.info("Body: {}", new String(request.body()));
                }
                log.info("====================================");
            }

            @Override
            protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
                log.info("========== Feign Response ==========");
                log.info("ConfigKey: {}", configKey);
                log.info("Status: {}", response.status());
                log.info("Headers: {}", response.headers());

                if (response.body() != null) {
                    byte[] bodyData = response.body().asInputStream().readAllBytes();
                    log.info("Body: {}", new String(bodyData));
                    log.info("=====================================");
                    return response.toBuilder().body(bodyData).build();
                }
                log.info("=====================================");
                return response;
            }
        };
    }
}