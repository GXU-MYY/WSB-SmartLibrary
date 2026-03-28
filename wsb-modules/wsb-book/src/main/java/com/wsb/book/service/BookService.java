package com.wsb.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookShelfDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.api.vo.MyBookListVO;
import com.wsb.book.api.vo.RecentBookVO;
import com.wsb.book.domain.Book;

import java.util.List;

/**
 * 图书服务接口
 */
public interface BookService extends IService<Book> {
    Page<BookVO> searchByCondition(Integer page, Integer pageSize, Long shelfId, String keyword, String classify);

    MyBookListVO getMyBooks();

    List<RecentBookVO> getRecentBooks();

    IsbnBookVO getBookByIsbn(String isbn);

    BookAddVO add(BookAddDTO dto);

    BookVO detail(Long bookId);

    BookVO update(BookUpdateDTO dto);

    void delete(Long id);

    void onShelf(BookShelfDTO dto);

    void offShelf(BookShelfDTO dto);
}
