package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.community.api.dto.GroupUserOperateDTO;
import com.wsb.community.api.vo.GroupUserVO;
import com.wsb.community.convert.GroupUserConverter;
import com.wsb.community.domain.Group;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.mapper.GroupMapper;
import com.wsb.community.mapper.GroupUserMapper;
import com.wsb.community.service.GroupUserService;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 群组成员服务实现类
 */
@Service
@RequiredArgsConstructor
public class GroupUserServiceImpl extends ServiceImpl<GroupUserMapper, GroupUser> implements GroupUserService {

    private final GroupUserConverter groupUserConverter;
    private final GroupMapper groupMapper;
    private final RemoteUserService remoteUserService;

    @Override
    public List<GroupUserVO> getGroupUsers(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验群组存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new ServiceException("群组不存在");
        }

        // 校验当前用户是否在群组中
        if (!isInGroup(groupId, currentUserId)) {
            throw new ServiceException("您不在该群组中，无权查看成员列表");
        }

        // 查询群组成员
        List<GroupUser> groupUsers = this.list(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getIsDeleted, false)
                .orderByDesc(GroupUser::getCreateTime));

        if (groupUsers.isEmpty()) {
            return List.of();
        }

        // 批量获取用户昵称
        List<Long> userIds = groupUsers.stream()
                .map(GroupUser::getUserId)
                .collect(Collectors.toList());
        Result<List<UserNicknameDTO>> nicknamesResult = remoteUserService.getUserNicknamesByIds(userIds);
        if (nicknamesResult == null || nicknamesResult.getData() == null) {
            return List.of();
        }

        Map<Long, UserNicknameDTO> userMap = nicknamesResult.getData().stream()
                .collect(Collectors.toMap(UserNicknameDTO::getId, u -> u, (a, b) -> a));

        // 组装VO
        return groupUsers.stream()
                .map(gu -> groupUserConverter.toGroupUserVO(gu, userMap.get(gu.getUserId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addUsers(GroupUserOperateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验群组存在
        Group group = groupMapper.selectById(dto.getGroupId());
        if (group == null || group.getIsDeleted()) {
            throw new ServiceException("群组不存在");
        }

        // 校验当前用户是否在群组中
        if (!isInGroup(dto.getGroupId(), currentUserId)) {
            throw new ServiceException("您不在该群组中，无权邀请成员");
        }

        // 校验用户是否存在
        remoteUserService.checkUserExists(dto.getUserIds());

        // 过滤已在群组的用户
        List<Long> existingUserIds = this.list(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, dto.getGroupId())
                .in(GroupUser::getUserId, dto.getUserIds())
                .eq(GroupUser::getIsDeleted, false))
                .stream()
                .map(GroupUser::getUserId)
                .collect(Collectors.toList());

        List<Long> newUserIds = dto.getUserIds().stream()
                .filter(id -> !existingUserIds.contains(id))
                .collect(Collectors.toList());

        if (newUserIds.isEmpty()) {
            throw new ServiceException("所有用户已在群组中");
        }

        // 批量插入
        List<GroupUser> groupUsers = newUserIds.stream()
                .map(userId -> {
                    GroupUser gu = new GroupUser();
                    gu.setGroupId(dto.getGroupId());
                    gu.setUserId(userId);
                    gu.setIsDeleted(false);
                    return gu;
                })
                .collect(Collectors.toList());
        this.saveBatch(groupUsers);
    }

    @Override
    public void removeUsers(GroupUserOperateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验群组存在
        Group group = groupMapper.selectById(dto.getGroupId());
        if (group == null || group.getIsDeleted()) {
            throw new ServiceException("群组不存在");
        }

        // 校验当前用户是否在群组中
        if (!isInGroup(dto.getGroupId(), currentUserId)) {
            throw new ServiceException("您不在该群组中，无权移除成员");
        }

        // 软删除
        this.update(Wrappers.<GroupUser>lambdaUpdate()
                .eq(GroupUser::getGroupId, dto.getGroupId())
                .in(GroupUser::getUserId, dto.getUserIds())
                .eq(GroupUser::getIsDeleted, false)
                .set(GroupUser::getIsDeleted, true));
    }

    @Override
    public boolean isInGroup(Long groupId, Long userId) {
        return this.exists(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getUserId, userId)
                .eq(GroupUser::getIsDeleted, false));
    }

    @Override
    public List<GroupUserVO> getNonGroupUsers(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验群组存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new ServiceException("群组不存在");
        }

        // 校验当前用户是否在群组中
        if (!isInGroup(groupId, currentUserId)) {
            throw new ServiceException("您不在该群组中，无权查看非成员列表");
        }

        // 获取群组已存在的成员ID
        java.util.Set<Long> existingUserIds = this.list(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getIsDeleted, false))
                .stream()
                .map(GroupUser::getUserId)
                .collect(Collectors.toSet());

        // 获取所有用户
        Result<List<UserNicknameDTO>> allUsersResult = remoteUserService.getAllUserNicknames();
        if (allUsersResult == null || allUsersResult.getData() == null) {
            return List.of();
        }

        // 过滤出非群组成员
        return allUsersResult.getData().stream()
                .filter(user -> !existingUserIds.contains(user.getId()))
                .map(user -> {
                    GroupUserVO vo = new GroupUserVO();
                    vo.setUserId(user.getId());
                    vo.setNickname(user.getNickName());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}