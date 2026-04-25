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
import com.wsb.community.service.support.CommunityCacheService;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群组成员服务实现
 */
@Service
@RequiredArgsConstructor
public class GroupUserServiceImpl extends ServiceImpl<GroupUserMapper, GroupUser> implements GroupUserService {

    private final GroupUserConverter groupUserConverter;
    private final GroupMapper groupMapper;
    private final RemoteUserService remoteUserService;
    private final CommunityCacheService communityCacheService;

    @Override
    public List<GroupUserVO> getGroupUsers(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Group group = requireGroupAccessible(groupId, currentUserId);

        List<GroupUser> groupUsers = this.list(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getIsDeleted, false)
                .orderByDesc(GroupUser::getCreateTime));

        if (groupUsers.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = groupUsers.stream()
                .map(GroupUser::getUserId)
                .toList();
        Result<List<UserNicknameDTO>> nicknamesResult = remoteUserService.getUserNicknamesByIds(userIds);
        if (nicknamesResult == null || nicknamesResult.getData() == null) {
            return List.of();
        }

        Map<Long, UserNicknameDTO> userMap = nicknamesResult.getData().stream()
                .collect(Collectors.toMap(UserNicknameDTO::getId, user -> user, (left, right) -> left));

        if (!userMap.containsKey(group.getOwnerId())) {
            UserNicknameDTO owner = new UserNicknameDTO();
            owner.setId(group.getOwnerId());
            owner.setNickName("群主");
            userMap.put(group.getOwnerId(), owner);
        }

        return groupUsers.stream()
                .map(groupUser -> groupUserConverter.toGroupUserVO(groupUser, userMap.get(groupUser.getUserId())))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUsers(GroupUserOperateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Group group = requireOwner(dto.getGroupId(), currentUserId);

        List<Long> targetUserIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(userId -> !Objects.equals(userId, group.getOwnerId()))
                .toList();
        if (targetUserIds.isEmpty()) {
            throw new ServiceException("没有可加入的成员");
        }

        Result<Void> checkResult = remoteUserService.checkUserExists(targetUserIds);
        if (checkResult.getCode() != 200) {
            throw new ServiceException(checkResult.getMsg());
        }

        Set<Long> existingUserIds = this.list(Wrappers.<GroupUser>lambdaQuery()
                        .eq(GroupUser::getGroupId, dto.getGroupId())
                        .in(GroupUser::getUserId, targetUserIds)
                        .eq(GroupUser::getIsDeleted, false))
                .stream()
                .map(GroupUser::getUserId)
                .collect(Collectors.toSet());

        List<Long> newUserIds = targetUserIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .toList();
        if (newUserIds.isEmpty()) {
            throw new ServiceException("所选用户均已在群组中");
        }

        List<GroupUser> groupUsers = newUserIds.stream()
                .map(userId -> {
                    GroupUser groupUser = new GroupUser();
                    groupUser.setGroupId(dto.getGroupId());
                    groupUser.setUserId(userId);
                    groupUser.setIsDeleted(false);
                    return groupUser;
                })
                .toList();
        this.saveBatch(groupUsers);
        communityCacheService.evictGroupPublicShelves(dto.getGroupId());
        communityCacheService.evictGroupPublicBooks(dto.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUsers(GroupUserOperateDTO dto) {
        List<Long> targetUserIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (targetUserIds.isEmpty()) {
            throw new ServiceException("请至少选择一名成员");
        }

        for (Long userId : targetUserIds) {
            kickUser(dto.getGroupId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickUser(Long groupId, Long userId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Group group = requireOwner(groupId, currentUserId);

        if (Objects.equals(userId, group.getOwnerId())) {
            throw new ServiceException("不能移除群主");
        }

        boolean removed = this.update(Wrappers.<GroupUser>lambdaUpdate()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getUserId, userId)
                .eq(GroupUser::getIsDeleted, false)
                .set(GroupUser::getIsDeleted, true));
        if (!removed) {
            throw new ServiceException("该成员已不在群组中");
        }
        communityCacheService.evictGroupPublicShelves(groupId);
        communityCacheService.evictGroupPublicBooks(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exitGroup(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Group group = requireGroupAccessible(groupId, currentUserId);

        if (Objects.equals(group.getOwnerId(), currentUserId)) {
            throw new ServiceException("群主不能退出群聊，只能解散群组");
        }

        boolean removed = this.update(Wrappers.<GroupUser>lambdaUpdate()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getUserId, currentUserId)
                .eq(GroupUser::getIsDeleted, false)
                .set(GroupUser::getIsDeleted, true));
        if (!removed) {
            throw new ServiceException("您当前不在该群组中");
        }
        communityCacheService.evictGroupPublicShelves(groupId);
        communityCacheService.evictGroupPublicBooks(groupId);
    }

    @Override
    public boolean isInGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group != null && !Boolean.TRUE.equals(group.getIsDeleted()) && Objects.equals(group.getOwnerId(), userId)) {
            return true;
        }
        return this.exists(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getUserId, userId)
                .eq(GroupUser::getIsDeleted, false));
    }

    @Override
    public List<GroupUserVO> getNonGroupUsers(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Group group = requireGroupAccessible(groupId, currentUserId);

        Set<Long> existingUserIds = new LinkedHashSet<>(this.list(Wrappers.<GroupUser>lambdaQuery()
                        .eq(GroupUser::getGroupId, groupId)
                        .eq(GroupUser::getIsDeleted, false))
                .stream()
                .map(GroupUser::getUserId)
                .collect(Collectors.toSet()));
        existingUserIds.add(group.getOwnerId());

        Result<List<UserNicknameDTO>> allUsersResult = remoteUserService.getAllUserNicknames();
        if (allUsersResult == null || allUsersResult.getData() == null) {
            return List.of();
        }

        return allUsersResult.getData().stream()
                .filter(user -> !existingUserIds.contains(user.getId()))
                .map(user -> {
                    GroupUserVO vo = new GroupUserVO();
                    vo.setUserId(user.getId());
                    vo.setNickname(user.getNickName());
                    vo.setAvatar(user.getAvatar());
                    return vo;
                })
                .toList();
    }

    private Group requireGroupAccessible(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getIsDeleted())) {
            throw new ServiceException("群组不存在");
        }
        if (!Objects.equals(group.getOwnerId(), userId) && !isInGroup(groupId, userId)) {
            throw new ServiceException("您不在该群组中，无权执行当前操作");
        }
        return group;
    }

    private Group requireOwner(Long groupId, Long userId) {
        Group group = requireGroupAccessible(groupId, userId);
        if (!Objects.equals(group.getOwnerId(), userId)) {
            throw new ServiceException("只有群主才能执行该操作");
        }
        return group;
    }
}
