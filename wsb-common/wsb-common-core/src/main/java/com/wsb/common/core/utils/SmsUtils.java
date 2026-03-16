package com.wsb.common.core.utils;

import com.aliyun.tea.TeaException;
import com.wsb.common.core.props.SmsProperties;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsUtils {

    private final Client dypnsClient;
    private final SmsProperties smsProperties;

    /**
     * 调用阿里云号码认证服务发送验证码
     * @param phone 目标手机号
     * @return 验证码 (如果设置了返回)
     */
    public String sendSmsVerifyCode(String phone) {
        try {
            // 组装请求参数
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setSchemeName(smsProperties.getSchemeName()) // 注意：需在号码认证控制台申请“方案名称”
                .setCountryCode("86")
                .setPhoneNumber(phone)
                .setTemplateCode(smsProperties.getTemplateCode())
                .setSignName(smsProperties.getSignName())
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}") // ##code## 是固定占位符
                .setCodeLength(6L)
                .setValidTime(300L)
                .setReturnVerifyCode(true); // 设置为 true 才能在 Response 中拿到 code

            // 发送
            SendSmsVerifyCodeResponse resp = dypnsClient.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());

            // 4. 处理结果
            if ("OK".equals(resp.getBody().getCode())) {
                String code = resp.getBody().getModel().getVerifyCode();
                log.info("短信验证码发送成功: {}, code: {}", phone, code);
                return code;
            } else {
                log.error("短信验证码发送失败: {}", resp.getBody().getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("阿里云号码认证接口调用异常", e);
            return null;
        }
    }

    /**
     * 调用阿里云核验验证码
     * @param phone 手机号
     * @param code 用户输入的验证码
     * @return 是否核验通过
     */
    public boolean checkVerifyCode(String phone, String code) {
        try {
            // 构造核验请求
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setSchemeName(smsProperties.getSchemeName()) // 必须与发送时一致
                .setCountryCode("86")
                .setPhoneNumber(phone)
                .setVerifyCode(code)
                .setCaseAuthPolicy(1L); // 1: 基础核验策略

            // 执行核验
            CheckSmsVerifyCodeResponse resp = dypnsClient.checkSmsVerifyCodeWithOptions(request, new RuntimeOptions());

            // 判断结果
            if (resp.getBody() != null && "OK".equals(resp.getBody().getCode())) {
                // 获取 Model 对象
                var model = resp.getBody().getModel();
                // 注意：阿里云返回的 VerifyResult 是 String 类型的 "true"
                if (model != null && "PASS".equals(String.valueOf(model.getVerifyResult()))) {
                    log.info("手机号 {} 验证码核验成功", phone);
                    return true;
                }
            }
            // 如果走到这里，说明核验失败
            String errorMsg = (resp.getBody() != null) ? resp.getBody().getMessage() : "接口响应为空";
            log.warn("手机号 {} 验证码核验失败: {}", phone, errorMsg);
            return false;
        } catch (Exception e) {
            log.error("调用阿里云验证码核验异常", e);
            return false;
        }
    }
}