package com.wsb.borrow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.borrow.api.dto.BookBorrowDTO;
import com.wsb.borrow.api.dto.BookBorrowUpdateDTO;
import com.wsb.borrow.api.dto.BookReturnDTO;
import com.wsb.borrow.api.vo.BookBorrowRecordVO;
import com.wsb.borrow.api.vo.BookBorrowVO;
import com.wsb.borrow.convert.BookBorrowConverter;
import com.wsb.borrow.domain.BookBorrow;
import com.wsb.borrow.mapper.BookBorrowMapper;
import com.wsb.borrow.service.BookBorrowService;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {

    private final RemoteBookService remoteBookService;
    private final BookBorrowConverter bookBorrowConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO borrow(BookBorrowDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 远程调用获取书籍信息
        Long bookId = dto.getBookId();
        Result<BookRemoteDTO> bookResult = remoteBookService.getBookById(bookId);
        if (bookResult == null || bookResult.getData() == null) {
            throw new ServiceException("书籍不存在");
        }
        BookRemoteDTO book = bookResult.getData();

        // 使用 Converter 转换并设置额外字段
        BookBorrow borrow = bookBorrowConverter.toBookBorrow(dto);
        borrow.setUserId(currentUserId);
        borrow.setBookName(book.getTitle());
        borrow.setCoverUrl(book.getCoverUrl());
        borrow.setIsDeleted(false);

        this.save(borrow);

        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO returning(BookReturnDTO dto) {
        Long bookId = Long.valueOf(dto.getBookId());
        Integer borrowType = Integer.valueOf(dto.getBorrowType());

        // 查找未归还的借书记录
        BookBorrow borrow = this.getOne(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, bookId)
                .eq(BookBorrow::getBorrowType, borrowType)
                .isNull(BookBorrow::getReturnTime)
                .orderByDesc(BookBorrow::getBorrowTime)
                .last("LIMIT 1"));

        if (borrow == null) {
            throw new ServiceException("未找到对应的借书记录");
        }

        // 解析还书时间
        LocalDate returnTime = LocalDate.parse(dto.getReturnTime());
        borrow.setReturnTime(returnTime);

        this.updateById(borrow);

        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    @Override
    public List<BookBorrowRecordVO> getRecords(Integer borrowType) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询借书记录
        List<BookBorrow> borrows;
        if (borrowType == null) {
            borrows = this.list(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getUserId, currentUserId));
        } else {
            borrows = this.list(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getUserId, currentUserId)
                    .eq(BookBorrow::getBorrowType, borrowType));
        }

        if (borrows.isEmpty()) {
            return List.of();
        }

        // 批量获取书籍信息
        List<Long> bookIds = borrows.stream().map(BookBorrow::getBookId).distinct().collect(Collectors.toList());
        Result<List<BookRemoteDTO>> booksResult = remoteBookService.getBooksByIds(bookIds);
        Map<Long, BookRemoteDTO> bookMap = Map.of();
        if (booksResult != null && booksResult.getData() != null) {
            bookMap = booksResult.getData().stream()
                    .collect(Collectors.toMap(BookRemoteDTO::getId, b -> b));
        }

        // 使用 Converter 转换
        List<BookBorrowRecordVO> voList = bookBorrowConverter.toBookBorrowRecordVOList(borrows);

        // 设置书籍信息
        final Map<Long, BookRemoteDTO> finalBookMap = bookMap;
        voList.forEach(vo -> {
            BookRemoteDTO book = finalBookMap.get(vo.getBookId());
            if (book != null) {
                vo.setTitle(book.getTitle());
                vo.setCoverUrl(book.getCoverUrl());
            }
        });

        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBorrow(BookBorrowUpdateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询借书记录
        BookBorrow borrow = this.getById(dto.getBorrowId());
        if (borrow == null) {
            throw new ServiceException("借书记录不存在");
        }

        // 校验是否是本人的借书记录
        if (!borrow.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权限修改他人借书记录");
        }

        // 使用 Converter 更新
        bookBorrowConverter.updateBookBorrowFromDto(dto, borrow);

        this.updateById(borrow);
    }
}