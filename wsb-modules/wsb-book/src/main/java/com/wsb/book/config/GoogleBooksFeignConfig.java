package com.wsb.book.config;

import feign.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Google Books API Feign 配置
 * 配置 HTTP 代理访问 Google
 */
@Slf4j
public class GoogleBooksFeignConfig {

    @Bean
    public Client feignClient(
            @Value("${google.books.proxy.host:}") String proxyHost,
            @Value("${google.books.proxy.port:0}") int proxyPort
    ) {
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            log.info("Google Books API 使用代理: {}:{}", proxyHost, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            return new Client.Proxied(null, null, proxy);
        }
        log.info("Google Books API 未配置代理，使用直连");
        return new Client.Default(null, null);
    }
}
