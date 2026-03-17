package com.wsb.social.convert;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.social.api.vo.CollectBookVO;
import com.wsb.social.api.vo.CollectShelfVO;
import com.wsb.social.api.vo.CollectVO;
import com.wsb.social.domain.Collect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 收藏转换器
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CollectConverter {

    // ========== Entity -> VO ==========

    @Mapping(target = "collectTime", source = "createTime")
    CollectVO toCollectVO(Collect collect);

    // ========== BookRemoteDTO -> CollectBookVO ==========

    @Mapping(target = "id", source = "collectId")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "pic", source = "book.coverUrl")
    @Mapping(target = "collectTime", source = "collectTime")
    CollectBookVO toCollectBookVO(BookRemoteDTO book, Long collectId, java.time.LocalDateTime collectTime);

    // ========== ShelfRemoteDTO -> CollectShelfVO ==========

    @Mapping(target = "id", source = "collectId")
    @Mapping(target = "shelfId", source = "shelf.id")
    @Mapping(target = "shelfName", source = "shelf.shelfName")
    @Mapping(target = "collectTime", source = "collectTime")
    CollectShelfVO toCollectShelfVO(ShelfRemoteDTO shelf, Long collectId, java.time.LocalDateTime collectTime);
}