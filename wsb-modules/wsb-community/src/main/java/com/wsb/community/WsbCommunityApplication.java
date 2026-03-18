package com.wsb.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.wsb.user.api", "com.wsb.book.api", "com.wsb.social.api"})
@MapperScan("com.wsb.community.mapper")
@ComponentScan(basePackages = "com.wsb")
public class WsbCommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsbCommunityApplication.class, args);
    }
}