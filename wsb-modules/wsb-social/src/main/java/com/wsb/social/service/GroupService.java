package com.wsb.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.social.api.dto.GroupAddDTO;
import com.wsb.social.api.dto.GroupUpdateDTO;
import com.wsb.social.api.vo.GroupVO;
import com.wsb.social.domain.Group;

public interface GroupService extends IService<Group> {
  GroupVO add(GroupAddDTO dto);

  GroupVO update(GroupUpdateDTO dto);

  void delete(Long groupId);
}
