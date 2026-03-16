package com.wsb.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.mapper.GroupUserMapper;
import com.wsb.community.service.GroupUserService;
import org.springframework.stereotype.Service;

/**
 * 群组成员服务实现类
 */
@Service
public class GroupUserServiceImpl extends ServiceImpl<GroupUserMapper, GroupUser> implements GroupUserService {
}