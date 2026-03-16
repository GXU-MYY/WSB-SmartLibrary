package com.wsb.book.convert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.MyBookVO;
import com.wsb.book.domain.Book;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.stream.Collectors;

/**
 * 图书转换器
 */
@Mapper(componentModel = "spring")
public interface BookConverter {

    Book toBook(BookAddDTO dto);

    BookAddVO toBookAddVO(Book book);

    BookVO toBookVO(Book book);

    MyBookVO toMyBookVO(Book book);

    default Page<BookVO> toVOPage(Page<Book> bookPage) {
        Page<BookVO> voPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        voPage.setRecords(bookPage.getRecords().stream()
                .map(this::toBookVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBookFromDto(BookUpdateDTO dto, @MappingTarget Book book);
}