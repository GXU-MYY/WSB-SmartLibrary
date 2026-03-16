package com.wsb.common.core.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wsb.sms")
public class SmsProperties {
    /** AccessKey ID */
    private String accessKeyId;
    /** AccessKey Secret */
    private String accessKeySecret;
    /** 签名名称 */
    private String signName;
    /** 模板代码 */
    private String templateCode;
    /** 方案名称 */
    private String schemeName;
}