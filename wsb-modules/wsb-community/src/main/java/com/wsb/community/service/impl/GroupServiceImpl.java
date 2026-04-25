package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.community.api.dto.GroupAddDTO;
import com.wsb.community.api.dto.GroupUpdateDTO;
import com.wsb.community.api.vo.GroupVO;
import com.wsb.community.convert.GroupConverter;
import com.wsb.community.domain.Group;
import com.wsb.community.domain.GroupBorrowRequest;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.mapper.GroupBorrowRequestMapper;
import com.wsb.community.mapper.GroupMapper;
import com.wsb.community.service.GroupService;
import com.wsb.community.service.GroupUserService;
import com.wsb.user.api.RemoteUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 群组服务实现
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final GroupUserService groupUserService;
    private final RemoteUserService remoteUserService;
    private final GroupConverter groupConverter;
    private final GroupBorrowRequestMapper groupBorrowRequestMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO add(GroupAddDTO dto) {
        List<Long> selectedUserIds = dto.getUserIds() == null
                ? List.of()
                : dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<Long> memberIds = Stream.concat(Stream.of(currentUserId), selectedUserIds.stream())
                .distinct()
                .toList();

        Result<Void> result = remoteUserService.checkUserExists(memberIds);
        if (result.getCode() != 200) {
            throw new ServiceException(result.getMsg());
        }

        Group group = new Group();
        group.setGroupName(dto.getGroupName());
        group.setOwnerId(currentUserId);
        group.setRemark(dto.getRemark());
        group.setIsDeleted(false);
        this.save(group);

        List<GroupUser> members = memberIds.stream()
                .map(userId -> new GroupUser(null, group.getId(), userId, false, null, null))
                .toList();
        groupUserService.saveBatch(members);

        return groupConverter.groupToVO(group);
    }

    @Override
    public GroupVO update(GroupUpdateDTO dto) {
        Group group = this.getById(dto.getGroupId());
        if (group == null || Boolean.TRUE.equals(group.getIsDeleted())) {
            throw new ServiceException("群组不存在");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(group.getOwnerId(), currentUserId)) {
            throw new ServiceException("无权限");
        }

        if (dto.getGroupName() != null) {
            group.setGroupName(dto.getGroupName());
        }
        if (dto.getRemark() != null) {
            group.setRemark(dto.getRemark());
        }
        this.updateById(group);

        return groupConverter.groupToVO(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        Group group = this.getById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getIsDeleted())) {
            throw new ServiceException("群组不存在");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(group.getOwnerId(), currentUserId)) {
            throw new ServiceException("无权限");
        }

        this.update(Wrappers.<Group>lambdaUpdate()
                .eq(Group::getId, groupId)
                .eq(Group::getIsDeleted, false)
                .set(Group::getIsDeleted, true));

        groupUserService.update(Wrappers.<GroupUser>lambdaUpdate()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getIsDeleted, false)
                .set(GroupUser::getIsDeleted, true));

        groupBorrowRequestMapper.update(null, Wrappers.<GroupBorrowRequest>lambdaUpdate()
                .eq(GroupBorrowRequest::getGroupId, groupId)
                .eq(GroupBorrowRequest::getIsDeleted, false)
                .set(GroupBorrowRequest::getIsDeleted, true));
    }
}
