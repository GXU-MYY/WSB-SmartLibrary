package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.constant.BookBorrowStatus;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowSummaryVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.convert.BookBorrowConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.service.BookBorrowService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 借阅服务实现类
 */
@Service
@RequiredArgsConstructor
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {

    private final BookMapper bookMapper;
    private final BookBorrowConverter bookBorrowConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO borrow(BookBorrowDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Long bookId = dto.getBookId();

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ServiceException("图书不存在");
        }

        validateBorrowDates(dto.getBorrowTime(), dto.getDueTime());
        validateActiveBorrow(bookId);

        BookBorrow borrow = bookBorrowConverter.toBookBorrow(dto);
        borrow.setUserId(currentUserId);
        borrow.setBookName(book.getTitle());
        borrow.setCoverUrl(book.getCoverUrl());
        borrow.setStatus(resolveBorrowStatus(dto.getDueTime(), null));
        borrow.setIsDeleted(false);

        this.save(borrow);
        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO returning(BookReturnDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        BookBorrow borrow = this.getOne(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getId, dto.getBorrowId())
                .eq(BookBorrow::getUserId, currentUserId)
                .last("LIMIT 1"));

        if (borrow == null) {
            throw new ServiceException("未找到对应的借阅记录");
        }

        if (BookBorrowStatus.RETURNED == borrow.getStatus()) {
            throw new ServiceException("该记录已归还，无需重复操作");
        }

        if (dto.getReturnTime().isBefore(borrow.getBorrowTime())) {
            throw new ServiceException("归还时间不能早于借阅时间");
        }

        borrow.setReturnTime(dto.getReturnTime());
        borrow.setStatus(BookBorrowStatus.RETURNED);
        this.updateById(borrow);

        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    @Override
    public Page<BookBorrowRecordVO> getRecords(Integer page, Integer pageSize, Integer borrowType, Integer status) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        syncOverdueStatus(currentUserId);

        Page<BookBorrow> borrowPage = this.page(
                new Page<>(page, pageSize),
                Wrappers.<BookBorrow>lambdaQuery()
                        .eq(BookBorrow::getUserId, currentUserId)
                        .eq(borrowType != null, BookBorrow::getBorrowType, borrowType)
                        .eq(status != null, BookBorrow::getStatus, status)
                        .orderByDesc(BookBorrow::getBorrowTime)
                        .orderByDesc(BookBorrow::getId)
        );

        List<BookBorrowRecordVO> records = buildBorrowRecordVOs(borrowPage.getRecords());
        Page<BookBorrowRecordVO> result = new Page<>(borrowPage.getCurrent(), borrowPage.getSize(), borrowPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public BookBorrowSummaryVO getSummary() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        syncOverdueStatus(currentUserId);

        List<BookBorrow> borrows = this.list(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, currentUserId));

        BookBorrowSummaryVO vo = new BookBorrowSummaryVO();
        vo.setTotal(borrows.size());
        vo.setBorrowedIn((int) borrows.stream().filter(item -> item.getBorrowType() != null && item.getBorrowType() == 1).count());
        vo.setBorrowedOut((int) borrows.stream().filter(item -> item.getBorrowType() != null && item.getBorrowType() == 2).count());
        vo.setActive((int) borrows.stream().filter(this::isActiveBorrow).count());
        vo.setOverdue((int) borrows.stream().filter(item -> item.getStatus() != null && item.getStatus() == BookBorrowStatus.OVERDUE).count());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBorrow(BookBorrowUpdateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        BookBorrow borrow = this.getOne(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getId, dto.getBorrowId())
                .eq(BookBorrow::getUserId, currentUserId)
                .last("LIMIT 1"));
        if (borrow == null) {
            throw new ServiceException("借阅记录不存在");
        }

        LocalDate targetBorrowTime = dto.getBorrowTime() != null ? dto.getBorrowTime() : borrow.getBorrowTime();
        LocalDate targetDueTime = dto.getDueTime() != null ? dto.getDueTime() : borrow.getDueTime();
        validateBorrowDates(targetBorrowTime, targetDueTime);

        if (borrow.getReturnTime() != null && borrow.getReturnTime().isBefore(targetBorrowTime)) {
            throw new ServiceException("借阅时间不能晚于实际归还时间");
        }

        bookBorrowConverter.updateBookBorrowFromDto(dto, borrow);
        if (borrow.getStatus() == null || borrow.getStatus() != BookBorrowStatus.RETURNED) {
            borrow.setStatus(resolveBorrowStatus(borrow.getDueTime(), borrow.getReturnTime()));
        }

        this.updateById(borrow);
    }

    private List<BookBorrowRecordVO> buildBorrowRecordVOs(List<BookBorrow> borrows) {
        if (borrows.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = borrows.stream().map(BookBorrow::getBookId).distinct().collect(Collectors.toList());
        Map<Long, Book> bookMap = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, item -> item));
        Map<Long, BookBorrow> borrowMap = borrows.stream()
                .collect(Collectors.toMap(BookBorrow::getId, item -> item));

        List<BookBorrowRecordVO> voList = bookBorrowConverter.toBookBorrowRecordVOList(borrows);
        voList.forEach(vo -> {
            Book book = bookMap.get(vo.getBookId());
            if (book != null) {
                vo.setTitle(book.getTitle());
                vo.setCoverUrl(book.getCoverUrl());
            } else {
                BookBorrow snapshot = borrowMap.get(vo.getId());
                if (snapshot != null) {
                    vo.setTitle(snapshot.getBookName());
                    vo.setCoverUrl(snapshot.getCoverUrl());
                }
            }
        });
        return voList;
    }

    private void validateActiveBorrow(Long bookId) {
        Long activeCount = this.baseMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, bookId)
                .eq(BookBorrow::getIsDeleted, false)
                .in(BookBorrow::getStatus, BookBorrowStatus.BORROWING, BookBorrowStatus.OVERDUE));
        if (activeCount != null && activeCount > 0) {
            throw new ServiceException("这本书当前已有未完成的借阅记录，请先归还后再重新登记");
        }
    }

    private void validateBorrowDates(LocalDate borrowTime, LocalDate dueTime) {
        if (borrowTime == null) {
            throw new ServiceException("借阅日期不能为空");
        }

        if (dueTime != null && dueTime.isBefore(borrowTime)) {
            throw new ServiceException("预计归还时间不能早于借阅时间");
        }
    }

    private int resolveBorrowStatus(LocalDate dueTime, LocalDate returnTime) {
        if (returnTime != null) {
            return BookBorrowStatus.RETURNED;
        }

        if (dueTime != null && dueTime.isBefore(LocalDate.now())) {
            return BookBorrowStatus.OVERDUE;
        }

        return BookBorrowStatus.BORROWING;
    }

    private boolean isActiveBorrow(BookBorrow borrow) {
        return borrow.getStatus() != null
                && borrow.getStatus() != BookBorrowStatus.RETURNED;
    }

    private void syncOverdueStatus(Long userId) {
        LocalDate today = LocalDate.now();

        this.update(Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getUserId, userId)
                .eq(BookBorrow::getIsDeleted, false)
                .eq(BookBorrow::getStatus, BookBorrowStatus.BORROWING)
                .isNotNull(BookBorrow::getDueTime)
                .lt(BookBorrow::getDueTime, today)
                .set(BookBorrow::getStatus, BookBorrowStatus.OVERDUE));

        this.update(Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getUserId, userId)
                .eq(BookBorrow::getIsDeleted, false)
                .eq(BookBorrow::getStatus, BookBorrowStatus.OVERDUE)
                .and(wrapper -> wrapper
                        .isNull(BookBorrow::getDueTime)
                        .or()
                        .ge(BookBorrow::getDueTime, today))
                .set(BookBorrow::getStatus, BookBorrowStatus.BORROWING));
    }
}
