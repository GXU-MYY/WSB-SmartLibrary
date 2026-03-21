package com.wsb.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wsb.book.api")
@ComponentScan(basePackages = "com.wsb.rag")
@EnableScheduling
public class WsbRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsbRagApplication.class, args);
    }
}