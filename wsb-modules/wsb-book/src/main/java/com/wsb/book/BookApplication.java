package com.wsb.book;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wsb.book.client")
@MapperScan("com.wsb.book.mapper")
@ComponentScan(basePackages = {"com.wsb.book", "com.wsb.common"})
public class BookApplication {
  public static void main(String[] args) {
    SpringApplication.run(BookApplication.class, args);
  }
}
