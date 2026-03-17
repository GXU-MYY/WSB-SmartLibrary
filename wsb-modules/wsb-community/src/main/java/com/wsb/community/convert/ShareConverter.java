package com.wsb.community.convert;

import com.wsb.community.api.vo.ShareRecordVO;
import com.wsb.community.api.vo.ShareVO;
import com.wsb.community.domain.Share;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 分享转换器
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShareConverter {

    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "sharePerson", source = "shareUserId")
    @Mapping(target = "shareTime", source = "createTime")
    ShareVO toShareVO(Share share);

    List<ShareVO> toShareVOList(List<Share> shares);

    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "sharePerson", source = "shareUserId")
    @Mapping(target = "shareTime", source = "createTime")
    ShareRecordVO toShareRecordVO(Share share);

    List<ShareRecordVO> toShareRecordVOList(List<Share> shares);
}