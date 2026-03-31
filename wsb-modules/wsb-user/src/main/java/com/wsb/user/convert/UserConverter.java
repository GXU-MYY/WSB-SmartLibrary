package com.wsb.user.convert;

import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.domain.User;
import org.mapstruct.Mapper;

/**
 * 用户转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserInfoVO toUserInfoVO(User user);

    UserRemoteDTO toUserRemoteDTO(User user);
}
