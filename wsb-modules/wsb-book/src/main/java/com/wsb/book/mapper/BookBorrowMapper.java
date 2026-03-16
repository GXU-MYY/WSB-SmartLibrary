package com.wsb.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wsb.book.domain.BookBorrow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookBorrowMapper extends BaseMapper<BookBorrow> {
}
