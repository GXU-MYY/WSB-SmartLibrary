package com.wsb.community.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.community.api.dto.GroupUserOperateDTO;
import com.wsb.community.api.vo.GroupUserVO;
import com.wsb.community.service.GroupUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群组成员控制器
 */
@Tag(name = "群组成员管理")
@RestController
@RequestMapping("/v1/group/user")
@RequiredArgsConstructor
public class GroupUserController {

    private final GroupUserService groupUserService;

    @Operation(summary = "群组成员列表", description = "查询群组内的成员列表，用户需在目标群组中")
    @GetMapping
    public Result<List<GroupUserVO>> getGroupUsers(
            @Parameter(description = "群组ID")
            @RequestParam("group_id") Long groupId,
            @Parameter(description = "类型：in-群组成员，out-非群组成员")
            @RequestParam("type") String type) {
        if ("in".equals(type)) {
            return Result.success(groupUserService.getGroupUsers(groupId));
        } else if ("out".equals(type)) {
            return Result.success(groupUserService.getNonGroupUsers(groupId));
        } else {
            return Result.error("type参数错误，可选值：in、out");
        }
    }

    @Operation(summary = "群组成员操作", description = "拉用户进群(type=add)或踢用户出群(type=minus)")
    @PostMapping
    public Result<Void> operateGroupUser(@Valid @RequestBody GroupUserOperateDTO dto) {
        if ("add".equals(dto.getType())) {
            groupUserService.addUsers(dto);
        } else if ("minus".equals(dto.getType())) {
            groupUserService.removeUsers(dto);
        } else {
            return Result.error("type参数错误，可选值：add、minus");
        }
        return Result.success();
    }
}