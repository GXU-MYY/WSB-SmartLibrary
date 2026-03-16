package com.wsb.borrow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.wsb.borrow.mapper")
@ComponentScan(basePackages = "com.wsb")
public class BorrowApplication {
  public static void main(String[] args) {
    SpringApplication.run(BorrowApplication.class, args);
  }
}
