package com.wsb.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.community.api.dto.GroupAddDTO;
import com.wsb.community.api.dto.GroupUpdateDTO;
import com.wsb.community.api.vo.GroupVO;
import com.wsb.community.domain.Group;

/**
 * 群组服务接口
 */
public interface GroupService extends IService<Group> {
    /**
     * 新增群组
     */
    GroupVO add(GroupAddDTO dto);

    /**
     * 更新群组
     */
    GroupVO update(GroupUpdateDTO dto);

    /**
     * 删除群组
     */
    void delete(Long groupId);
}