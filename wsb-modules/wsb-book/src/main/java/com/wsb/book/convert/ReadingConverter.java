package com.wsb.book.convert;

import com.wsb.book.api.vo.BookReadingVO;
import com.wsb.book.domain.BookReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 阅读记录转换器
 */
@Mapper(componentModel = "spring")
public interface ReadingConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bookName", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    BookReading toBookReading(Long bookId, Integer readingStatus);

    BookReadingVO toBookReadingVO(BookReading bookReading);

    List<BookReadingVO> toBookReadingVOList(List<BookReading> bookReadings);
}