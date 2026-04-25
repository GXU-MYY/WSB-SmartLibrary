package com.wsb.community.convert;

import com.wsb.community.api.vo.GroupUserVO;
import com.wsb.community.domain.GroupUser;
import com.wsb.user.api.dto.UserNicknameDTO;
import org.mapstruct.Mapper;

/**
 * 群组成员转换器
 */
@Mapper(componentModel = "spring")
public interface GroupUserConverter {

    /**
     * 组装群组成员 VO（附带昵称与头像）
     */
    default GroupUserVO toGroupUserVO(GroupUser groupUser, UserNicknameDTO user) {
        if (groupUser == null || user == null) {
            return null;
        }
        GroupUserVO vo = new GroupUserVO();
        vo.setId(groupUser.getId());
        vo.setUserId(groupUser.getUserId());
        vo.setNickname(user.getNickName());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
