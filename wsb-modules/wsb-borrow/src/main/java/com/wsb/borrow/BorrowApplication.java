package com.wsb.borrow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wsb")
@MapperScan("com.wsb.borrow.mapper")
@ComponentScan(basePackages = "com.wsb")
public class BorrowApplication {
  public static void main(String[] args) {
    SpringApplication.run(BorrowApplication.class, args);
  }
}
