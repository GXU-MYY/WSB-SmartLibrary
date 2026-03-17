package com.wsb.social;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.wsb.user.api", "com.wsb.book.api"})
@MapperScan("com.wsb.social.mapper")
@ComponentScan(basePackages = "com.wsb")
public class SocialApplication {
  public static void main(String[] args) {
    SpringApplication.run(SocialApplication.class, args);
  }
}
