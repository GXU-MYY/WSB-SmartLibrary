package com.wsb.community.convert;

import com.wsb.community.api.vo.GroupVO;
import com.wsb.community.domain.Group;
import org.mapstruct.Mapper;

/**
 * 群组转换器
 */
@Mapper(componentModel = "spring")
public interface GroupConverter {

    GroupVO groupToVO(Group group);
}