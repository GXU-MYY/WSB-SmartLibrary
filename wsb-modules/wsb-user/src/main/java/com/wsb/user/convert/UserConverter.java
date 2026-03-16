package com.wsb.user.convert;

import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.api.dto.UserUpdateDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.domain.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 用户转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);

    UserInfoVO toUserInfoVO(User user);

    UserRemoteDTO toUserRemoteDTO(User user);
}