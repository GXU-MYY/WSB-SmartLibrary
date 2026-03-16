package com.wsb.book.service.impl;

import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BookService 集成测试
 */
@SpringBootTest
class BookServiceImplTest {

    @Autowired
    private BookService bookService;

    @Test
    void getBookByIsbn() {
        // 测试一个真实的 ISBN
        String isbn = "9787111544937";

        IsbnBookVO result = bookService.getBookByIsbn(isbn);

        System.out.println("========== 图书信息 ==========");
        System.out.println("书名: " + result.getTitle());
        System.out.println("作者: " + result.getAuthor());
        System.out.println("出版社: " + result.getPublisher());
        System.out.println("出版日期: " + result.getPublishDate());
        System.out.println("价格: " + result.getPrice());
        System.out.println("装帧: " + result.getBinding());
        System.out.println("ISBN: " + result.getIsbn());
        System.out.println("ISBN-10: " + result.getIsbn10());
        System.out.println("封面: " + result.getCoverUrl());
        System.out.println("简介: " + result.getSummary());
        System.out.println("==============================");
    }
}