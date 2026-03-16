package com.wsb.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.common.core.domain.Result;
import com.wsb.community.api.dto.GroupAddDTO;
import com.wsb.community.api.dto.GroupUpdateDTO;
import com.wsb.community.api.vo.GroupVO;
import com.wsb.community.domain.Group;
import com.wsb.community.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 群组控制器
 */
@Tag(name = "群组管理")
@RestController
@RequestMapping("/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "查询群组列表")
    @GetMapping
    public Result<Page<Group>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer page_size,
            @RequestParam(required = false) String name) {
        Page<Group> pageParam = new Page<>(page, page_size);
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        if (name != null) {
            wrapper.like(Group::getGroupName, name);
        }
        return Result.success(groupService.page(pageParam, wrapper));
    }

    @Operation(summary = "新增群组")
    @PostMapping
    public Result<GroupVO> add(@RequestBody GroupAddDTO dto) {
        GroupVO groupVO = groupService.add(dto);
        return Result.success(groupVO);
    }

    @Operation(summary = "修改群组信息")
    @PutMapping
    public Result<GroupVO> update(@RequestBody GroupUpdateDTO dto) {
        GroupVO groupVO = groupService.update(dto);
        return Result.success(groupVO);
    }

    @Operation(summary = "删除群组")
    @DeleteMapping
    public Result<Void> delete(@RequestParam("group_id") Long groupId) {
        groupService.delete(groupId);
        return Result.success();
    }
}