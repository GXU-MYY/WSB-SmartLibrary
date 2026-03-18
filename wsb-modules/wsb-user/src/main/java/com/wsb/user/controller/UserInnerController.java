package com.wsb.user.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.user.api.dto.UserNicknameDTO;
import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户内部接口控制器
 */
@Tag(name = "User Inner API")
@RestController
@RequestMapping("/v1/inner")
@RequiredArgsConstructor
public class UserInnerController {

    private final UserService userService;

    @Operation(summary = "Internal call - get user info by username", hidden = true)
    @GetMapping("/info/{username}")
    public Result<UserRemoteDTO> getUserInfoByUsername(@PathVariable("username") String username) {
        return Result.success(userService.getUserInfoByUsername(username));
    }

    @Operation(summary = "Internal call - check if users exist by id", hidden = true)
    @GetMapping("/exists")
    public Result<Void> checkUserExists(@RequestParam("user_ids") List<Long> userIds) {
        userService.existsByIds(userIds);
        return Result.success();
    }

    @Operation(summary = "Internal call - get user nicknames by ids", hidden = true)
    @GetMapping("/nicknames")
    public Result<List<UserNicknameDTO>> getUserNicknamesByIds(@RequestParam("ids") List<Long> userIds) {
        return Result.success(userService.getUserNicknamesByIds(userIds));
    }

    @Operation(summary = "Internal call - get all user nicknames", hidden = true)
    @GetMapping("/nicknames/all")
    public Result<List<UserNicknameDTO>> getAllUserNicknames() {
        return Result.success(userService.getAllUserNicknames());
    }
}