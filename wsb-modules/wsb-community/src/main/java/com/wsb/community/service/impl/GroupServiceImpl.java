package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.community.api.dto.GroupAddDTO;
import com.wsb.community.api.dto.GroupUpdateDTO;
import com.wsb.community.api.vo.GroupVO;
import com.wsb.community.convert.GroupConverter;
import com.wsb.community.domain.Group;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.mapper.GroupMapper;
import com.wsb.community.service.GroupService;
import com.wsb.community.service.GroupUserService;
import com.wsb.user.api.RemoteUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 群组服务实现类
 */
@Service
@AllArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final GroupUserService groupUserService;
    private final RemoteUserService remoteUserService;
    private final GroupConverter groupConverter;

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
        if (result.getCode() != 200) {
            throw new ServiceException(result.getMsg());
        }

        // 3. 获取当前登录人作为 Owner
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 4. 保存群组表记录
        Group group = new Group();
        group.setGroupName(dto.getGroupName());
        group.setOwnerId(currentUserId);
        group.setRemark(dto.getRemark());
        group.setIsDeleted(false);
        this.save(group);

        // 5. 批量保存成员记录
        List<GroupUser> members = dto.getUserIds().stream()
                .map(uid -> new GroupUser(null, group.getId(), uid, false, null, null))
                .toList();
        groupUserService.saveBatch(members);

        return groupConverter.groupToVO(group);
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

        // 3. 更新群组信息
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
        groupUserService.remove(new LambdaQueryWrapper<GroupUser>()
                .eq(GroupUser::getGroupId, groupId));

        // 4. 删除群组记录
        this.removeById(groupId);
    }
}