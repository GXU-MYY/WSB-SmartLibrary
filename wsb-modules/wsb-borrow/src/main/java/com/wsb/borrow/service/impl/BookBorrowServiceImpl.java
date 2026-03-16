package com.wsb.borrow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.borrow.domain.BookBorrow;
import com.wsb.borrow.mapper.BookBorrowMapper;
import com.wsb.borrow.service.BookBorrowService;
import org.springframework.stereotype.Service;

@Service
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {
}
