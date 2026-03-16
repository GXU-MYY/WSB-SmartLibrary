package com.wsb.common.core.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.wsb.common.core.props.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AliyunSmsConfig {

    private final SmsProperties smsProperties;

    @Bean
    public Client dypnsClient() throws Exception {
        Config config = new Config()
            .setAccessKeyId(smsProperties.getAccessKeyId())
            .setAccessKeySecret(smsProperties.getAccessKeySecret())
            .setEndpoint("dypnsapi.aliyuncs.com");
        return new Client(config);
    }
}