package com.wsb.social.convert;


import com.wsb.social.api.vo.GroupVO;
import com.wsb.social.domain.Group;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface GroupConverter {

  GroupVO groupToVO(Group group);
}
