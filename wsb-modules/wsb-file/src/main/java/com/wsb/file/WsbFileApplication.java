package com.wsb.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.wsb.file")
public class WsbFileApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsbFileApplication.class, args);
    }
}