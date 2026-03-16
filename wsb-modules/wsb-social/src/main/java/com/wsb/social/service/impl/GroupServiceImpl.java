package com.wsb.social.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.social.api.dto.GroupAddDTO;
import com.wsb.social.api.dto.GroupUpdateDTO;
import com.wsb.social.api.vo.GroupVO;
import com.wsb.social.domain.Group;
import com.wsb.social.domain.GroupMember;
import com.wsb.social.mapper.GroupMapper;
import com.wsb.social.mapper.GroupMemberMapper;
import com.wsb.social.service.GroupMemberService;
import com.wsb.social.service.GroupService;
import com.wsb.user.api.RemoteUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final GroupMemberService groupMemberService;
    private final RemoteUserService remoteUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO add(GroupAddDTO dto) {
        // 1. 校验群成员是否为空
        List<Long> userIds = dto.getUserIds().stream().distinct().toList();
        if (userIds.isEmpty()) {
            throw new ServiceException("群成员不能为空");
        }

        // 2. 校验群成员是否存在
        Result<Void> result = remoteUserService.checkUserExists(userIds);
        if(result.getCode() != 200) {
            throw new ServiceException(result.getMsg());
        }

        // 3. 获取当前登录人作为 Owner
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 4. 保存群主表记录
        Group group = new Group();
        group.setGroupName(dto.getName());
        group.setOwnerId(currentUserId);
        this.save(group);

        // 5. 批量保存成员记录
        List<GroupMember> members = dto.getUserIds().stream().map(uid -> new GroupMember(null, group.getId(), uid)).toList();
        groupMemberService.saveBatch(members);

        GroupVO groupVO = new GroupVO(group.getGroupName(), group.getId(), group.getOwnerId(), null, null);
        return groupVO;
    }

    @Override
    public GroupVO update(GroupUpdateDTO dto) {
        // 1. 校验群组是否存在
        Group group = this.getById(dto.getGroupId());
        if (group == null) {
            throw new ServiceException("群组不存在");
        }

        // 2. 校验当前登录人是否为群主
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new ServiceException("无权限");
        }

        // 3. 更新群组名称
        group.setGroupName(dto.getGroupName());
        this.updateById(group);

        GroupVO groupVO = new GroupVO(group.getGroupName(), group.getId(), group.getOwnerId(), group.getRemark(), null);
        return groupVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        // 1. 校验群组是否存在
        Group group = this.getById(groupId);
        if (group == null) {
            throw new ServiceException("群组不存在");
        }

        // 2. 校验当前登录人是否为群主
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new ServiceException("无权限");
        }

        // 3. 删除群组成员记录
        groupMemberService.remove(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupId, groupId));

        // 4. 删除群组记录
        this.removeById(groupId);
    }
}
