package com.wsb.book.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.book.api.dto.BookReadingAddDTO;
import com.wsb.book.api.dto.BookReadingUpdateDTO;
import com.wsb.book.api.vo.BookReadingVO;
import com.wsb.book.domain.BookReading;

import java.util.List;

/**
 * 阅读记录服务接口
 */
public interface ReadingService extends IService<BookReading> {

    /**
     * 新增阅读记录
     */
    BookReadingVO add(BookReadingAddDTO dto);

    /**
     * 更新阅读记录
     */
    BookReadingVO update(BookReadingUpdateDTO dto);

    /**
     * 查询当前用户指定书籍的阅读记录
     */
    BookReadingVO getByBookId(Long bookId);

    /**
     * 查询当前用户所有阅读记录
     */
    List<BookReadingVO> listAll();
}