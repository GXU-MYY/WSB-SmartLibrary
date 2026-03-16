package com.wsb.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wsb.community.domain.Group;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组Mapper
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
}