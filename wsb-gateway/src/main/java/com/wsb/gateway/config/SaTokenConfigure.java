package com.wsb.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关权限配置
 */
@Configuration
public class SaTokenConfigure {

  @Bean
  public SaReactorFilter getSaReactorFilter() {
    return new SaReactorFilter()
        // 拦截地址
        .addInclude("/**")
        // 开放地址
        .addExclude("/favicon.ico")
        // 鉴权方法：每次请求进入时触发
        .setAuth(obj -> {
          // 登录校验 -- 拦截所有路由，并排除登录、注册、验证码等开放接口
          SaRouter.match("/**")
              .notMatch("/auth/**") // 旧版登录路径(保留兼容)
              .notMatch("/v1/admin/login") // 登录
              .notMatch("/v1/admin/register") // 注册
              .notMatch("/v1/admin/passwd") // 忘记密码
              .notMatch("/v1/admin/phone/**") // 验证码
              .notMatch("/doc.html") // Swagger文档
              .notMatch("/webjars/**") // Swagger资源
              .notMatch("/v3/api-docs/**") // Swagger API定义
              .notMatch("/*/v3/api-docs") // 微服务API定义
              .notMatch("/swagger-resources/**") // Swagger资源
              .check(r -> StpUtil.checkLogin());
          // 权限认证 -- 不同模块, 校验不同权限
          // SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
        })
        // 异常处理方法：每次setAuth函数出现异常时触发
        .setError(e -> {
          return SaResult.error(e.getMessage());
        });
  }
}
