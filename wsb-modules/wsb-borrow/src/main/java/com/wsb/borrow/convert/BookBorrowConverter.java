package com.wsb.borrow.convert;

import com.wsb.borrow.api.dto.BookBorrowDTO;
import com.wsb.borrow.api.dto.BookBorrowUpdateDTO;
import com.wsb.borrow.api.vo.BookBorrowRecordVO;
import com.wsb.borrow.api.vo.BookBorrowVO;
import com.wsb.borrow.domain.BookBorrow;
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
    @Mapping(target = "bookName", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    BookBorrow toBookBorrow(BookBorrowDTO dto);

    /**
     * 实体转换为 VO
     */
    BookBorrowVO toBookBorrowVO(BookBorrow entity);

    /**
     * 实体转换为记录 VO
     */
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    BookBorrowRecordVO toBookBorrowRecordVO(BookBorrow entity);

    /**
     * 实体列表转换为记录 VO 列表
     */
    List<BookBorrowRecordVO> toBookBorrowRecordVOList(List<BookBorrow> entities);

    /**
     * 更新实体（从 DTO）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "returnTime", ignore = true)
    @Mapping(target = "borrowType", ignore = true)
    @Mapping(target = "bookName", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateBookBorrowFromDto(BookBorrowUpdateDTO dto, @MappingTarget BookBorrow entity);
}