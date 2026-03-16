package com.wsb.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wsb.borrow.domain.BookBorrow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookBorrowMapper extends BaseMapper<BookBorrow> {
}
