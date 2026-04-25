package com.wsb.book.convert;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.domain.Book;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 图书内部接口转换器
 */
@Mapper(componentModel = "spring")
public interface BookInnerConverter {

    BookRemoteDTO toBookRemoteDTO(Book book);

    List<BookRemoteDTO> toBookRemoteDTOList(List<Book> books);
}
