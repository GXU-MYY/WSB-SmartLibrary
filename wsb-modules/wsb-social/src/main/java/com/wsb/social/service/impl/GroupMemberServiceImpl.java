package com.wsb.social.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.social.domain.GroupMember;
import com.wsb.social.mapper.GroupMemberMapper;
import com.wsb.social.service.GroupMemberService;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements GroupMemberService {
}
