package com.wsb.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.common.core.domain.Result;
import com.wsb.user.api.dto.*;
import com.wsb.user.convert.UserConverter;
import com.wsb.user.api.dto.UserRegisterDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Management")
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "User registration")
  @PostMapping("/register")
  public Result<UserInfoVO> register(@RequestBody UserRegisterDTO dto) {
    return Result.success(userService.register(dto));
  }

  @Operation(summary = "Update user info")
  @PutMapping("/user")
  public Result<UserInfoVO> update(@RequestBody UserUpdateDTO dto) {
    return Result.success(userService.updateUserInfo(dto));
  }

  @Operation(summary = "Query user list/detail")
  @GetMapping("/user")
  public Result<?> getUser(
      @RequestParam(value = "user_id", required = false) Long userId,
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10") Integer pageSize,
      @RequestParam(value = "user_name", required = false) String userName) {

    // Detail
    if (userId != null) {
      return Result.success(userService.getUserInfoByUserId(userId));
    }

    // List
    Page<UserInfoVO> userPage = userService.getUserList(page, pageSize, userName);
    return Result.success(userPage);
  }

  @Operation(summary = "Reset user password")
  @PostMapping("/passwd")
  public Result<Void> resetPassword(@RequestBody UserResetPwdDTO dto) {
    userService.resetPassword(dto);
    return Result.success();
  }

  @Operation(summary = "Send SMS verification code")
  @GetMapping("/phone/captcha")
  public Result<Void> sendCaptcha(@RequestParam("phone") String phone) {
    userService.sendCaptcha(phone);
    return Result.success();
  }
}