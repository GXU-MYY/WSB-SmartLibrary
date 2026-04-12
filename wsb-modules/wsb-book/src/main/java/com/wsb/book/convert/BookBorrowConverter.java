package com.wsb.book.convert;

import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.domain.BookBorrow;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * 借阅记录转换器
 */
@Mapper(componentModel = "spring")
public interface BookBorrowConverter {

    /**
     * DTO 转换为实体（新增借阅）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "returnTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bookName", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    BookBorrow toBookBorrow(BookBorrowDTO dto);

    /**
     * 实体转换为VO
     */
    BookBorrowVO toBookBorrowVO(BookBorrow entity);

    /**
     * 实体转换为记录VO
     */
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    BookBorrowRecordVO toBookBorrowRecordVO(BookBorrow entity);

    /**
     * 实体列表转换为记录VO列表
     */
    List<BookBorrowRecordVO> toBookBorrowRecordVOList(List<BookBorrow> entities);

    /**
     * 更新实体（从DTO）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "returnTime", ignore = true)
    @Mapping(target = "borrowType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bookName", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateBookBorrowFromDto(BookBorrowUpdateDTO dto, @MappingTarget BookBorrow entity);
}
