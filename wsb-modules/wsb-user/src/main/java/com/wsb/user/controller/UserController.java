package com.wsb.user.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.common.core.domain.Result;
import com.wsb.user.api.dto.*;
import com.wsb.user.convert.UserConverter;
import com.wsb.user.api.dto.UserLoginDTO;
import com.wsb.user.api.dto.UserRegisterDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "用户登录")
  @PostMapping("/login")
  public Result<SaTokenInfo> login(@Validated @RequestBody UserLoginDTO dto) {
    return Result.success(userService.login(dto));
  }

  @Operation(summary = "用户注销")
  @PostMapping("/logout")
  public Result<Void> logout() {
    userService.logout();
    return Result.success();
  }

  @Operation(summary = "用户注册")
  @PostMapping("/register")
  public Result<UserInfoVO> register(@RequestBody UserRegisterDTO dto) {
    return Result.success(userService.register(dto));
  }

  @Operation(summary = "更新用户信息")
  @PutMapping("/user")
  public Result<UserInfoVO> update(@Validated @RequestBody UserUpdateDTO dto) {
    return Result.success(userService.updateUserInfo(dto));
  }

  @Operation(summary = "查询用户列表/详情")
  @GetMapping("/user")
  public Result<?> getUser(
      @RequestParam(value = "user_id", required = false) Long userId,
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10") Integer pageSize,
      @RequestParam(value = "user_name", required = false) String userName) {

    // 详情
    if (userId != null) {
      return Result.success(userService.getUserInfoByUserId(userId));
    }

    // 列表
    Page<UserInfoVO> userPage = userService.getUserList(page, pageSize, userName);
    return Result.success(userPage);
  }

  @Operation(summary = "重置用户密码")
  @PostMapping("/passwd")
  public Result<Void> resetPassword(@RequestBody UserResetPwdDTO dto) {
    userService.resetPassword(dto);
    return Result.success();
  }

  @Operation(summary = "发送短信验证码")
  @GetMapping("/phone/captcha")
  public Result<Void> sendCaptcha(@RequestParam("phone") String phone) {
    userService.sendCaptcha(phone);
    return Result.success();
  }
}
