package com.wsb.auth.controller;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.wsb.common.core.domain.Result;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.api.dto.UserLoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心")
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AuthController {

  private final RemoteUserService remoteUserService;

  @Operation(summary = "登录")
  @PostMapping("/login")
  public Result<SaTokenInfo> login(@Validated @RequestBody UserLoginDTO loginDTO) {
    // 1. 调用用户服务获取用户信息 (仅支持用户名)
    Result<UserRemoteDTO> userResult = remoteUserService.getUserInfoByUsername(loginDTO.getUsername());
    if (userResult == null || userResult.getData() == null) {
      return Result.error("用户不存在");
    }

    UserRemoteDTO user = userResult.getData();

    // 2. 校验密码
    if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
      return Result.error("密码错误");
    }

    // 3. 校验状态
    if (!Boolean.TRUE.equals(user.getIsActive())) {
      return Result.error("账号未激活");
    }

    // 4. 登录
    StpUtil.login(user.getId());
    return Result.success(StpUtil.getTokenInfo());
  }

  @Operation(summary = "注销")
  @PostMapping("/logout")
  public Result<Void> logout() {
    StpUtil.logout();
    return Result.success();
  }
}
