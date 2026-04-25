package com.wsb.book.convert;

import com.wsb.book.api.vo.CollectBookVO;
import com.wsb.book.api.vo.CollectVO;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.Collect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;

/**
 * 收藏转换器
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CollectConverter {

    @Mapping(target = "collectTime", source = "createTime")
    CollectVO toCollectVO(Collect collect);

    @Mapping(target = "id", source = "collectId")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "author", source = "book.author")
    @Mapping(target = "pic", source = "book.coverUrl")
    @Mapping(target = "collectTime", source = "collectTime")
    CollectBookVO toCollectBookVO(Book book, Long collectId, LocalDateTime collectTime);
}
