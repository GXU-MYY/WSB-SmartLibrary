package com.wsb.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.community.api.dto.GroupUserOperateDTO;
import com.wsb.community.api.vo.GroupUserVO;
import com.wsb.community.domain.GroupUser;

import java.util.List;

/**
 * 群组成员服务接口
 */
public interface GroupUserService extends IService<GroupUser> {

    /**
     * 获取群组成员列表
     *
     * @param groupId 群组ID
     * @return 成员列表
     */
    List<GroupUserVO> getGroupUsers(Long groupId);

    /**
     * 拉用户进群
     *
     * @param dto 操作DTO
     */
    void addUsers(GroupUserOperateDTO dto);

    /**
     * 将用户移出群聊，仅群主可用
     *
     * @param dto 操作DTO
     */
    void removeUsers(GroupUserOperateDTO dto);

    /**
     * 群主移除指定成员
     *
     * @param groupId 群组ID
     * @param userId  用户ID
     */
    void kickUser(Long groupId, Long userId);

    /**
     * 当前登录用户退出群聊
     *
     * @param groupId 群组ID
     */
    void exitGroup(Long groupId);

    /**
     * 检查用户是否在群组中
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否在群组中
     */
    boolean isInGroup(Long groupId, Long userId);

    /**
     * 获取非群组成员列表
     *
     * @param groupId 群组ID
     * @return 非成员列表
     */
    List<GroupUserVO> getNonGroupUsers(Long groupId);
}
