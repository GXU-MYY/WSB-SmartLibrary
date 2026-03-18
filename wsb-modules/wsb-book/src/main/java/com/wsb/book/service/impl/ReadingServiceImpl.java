package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.BookReadingAddDTO;
import com.wsb.book.api.dto.BookReadingUpdateDTO;
import com.wsb.book.api.vo.BookReadingVO;
import com.wsb.book.convert.ReadingConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookReading;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.mapper.BookReadingMapper;
import com.wsb.book.service.ReadingService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 阅读记录服务实现类
 */
@Service
@RequiredArgsConstructor
public class ReadingServiceImpl extends ServiceImpl<BookReadingMapper, BookReading> implements ReadingService {

    private final ReadingConverter readingConverter;
    private final BookMapper bookMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookReadingVO add(BookReadingAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long bookId = dto.getBookId();

        // 校验书籍是否存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        // 查询是否已存在阅读记录
        BookReading existingRecord = this.getOne(Wrappers.<BookReading>lambdaQuery()
                .eq(BookReading::getUserId, userId)
                .eq(BookReading::getBookId, bookId));

        if (existingRecord != null) {
            // 已存在则更新状态
            existingRecord.setReadingStatus(dto.getReadingStatus());
            existingRecord.setBookName(book.getTitle());
            existingRecord.setCoverUrl(book.getCoverUrl());
            this.updateById(existingRecord);
            return readingConverter.toBookReadingVO(existingRecord);
        }

        // 不存在则新增
        BookReading bookReading = new BookReading();
        bookReading.setUserId(userId);
        bookReading.setBookId(bookId);
        bookReading.setReadingStatus(dto.getReadingStatus());
        bookReading.setBookName(book.getTitle());
        bookReading.setCoverUrl(book.getCoverUrl());
        bookReading.setIsDeleted(false);
        this.save(bookReading);

        return readingConverter.toBookReadingVO(bookReading);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookReadingVO update(BookReadingUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long bookId = dto.getBookId();

        // 查询阅读记录
        BookReading bookReading = this.getOne(Wrappers.<BookReading>lambdaQuery()
                .eq(BookReading::getUserId, userId)
                .eq(BookReading::getBookId, bookId));

        if (bookReading == null) {
            throw new ServiceException("阅读记录不存在");
        }

        // 更新状态
        bookReading.setReadingStatus(dto.getReadingStatus());
        this.updateById(bookReading);

        return readingConverter.toBookReadingVO(bookReading);
    }

    @Override
    public BookReadingVO getByBookId(Long bookId) {
        Long userId = StpUtil.getLoginIdAsLong();

        BookReading bookReading = this.getOne(Wrappers.<BookReading>lambdaQuery()
                .eq(BookReading::getUserId, userId)
                .eq(BookReading::getBookId, bookId));

        return readingConverter.toBookReadingVO(bookReading);
    }

    @Override
    public List<BookReadingVO> listAll() {
        Long userId = StpUtil.getLoginIdAsLong();

        List<BookReading> bookReadings = this.list(Wrappers.<BookReading>lambdaQuery()
                .eq(BookReading::getUserId, userId)
                .orderByDesc(BookReading::getUpdateTime));

        return readingConverter.toBookReadingVOList(bookReadings);
    }
}