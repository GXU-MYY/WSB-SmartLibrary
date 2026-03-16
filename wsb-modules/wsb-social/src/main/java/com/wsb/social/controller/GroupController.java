package com.wsb.social.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.common.core.domain.Result;
import com.wsb.social.api.dto.GroupAddDTO;
import com.wsb.social.api.dto.GroupUpdateDTO;
import com.wsb.social.api.vo.GroupVO;
import com.wsb.social.domain.Group;
import com.wsb.social.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "群组管理")
@RestController
@RequestMapping("/v1/group")
@RequiredArgsConstructor
public class GroupController {

  private final GroupService groupService;

  // todo
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

  @Operation
  @DeleteMapping
  public Result<Void> delete(@RequestParam("group_id") Long groupId) {
    groupService.delete(groupId);
    return Result.success();
  }
}
