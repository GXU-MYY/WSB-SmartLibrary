package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.service.BookBorrowService;
import com.wsb.common.core.exception.ServiceException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {

    private final BookMapper bookMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO borrow(BookBorrowDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验书籍是否存在
        Long bookId = dto.getBookId();
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        // 创建借书记录
        BookBorrow borrow = new BookBorrow();
        borrow.setBookId(bookId);
        borrow.setUserId(currentUserId);
        borrow.setBorrowerName(dto.getBorrowerName());
        borrow.setBorrowType(dto.getBorrowType());
        borrow.setBorrowTime(dto.getBorrowTime());
        borrow.setBookName(book.getTitle());
        borrow.setCoverUrl(book.getCoverUrl());
        borrow.setIsDeleted(false);

        this.save(borrow);

        // 转换为VO返回
        return toVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO returning(BookReturnDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验书籍是否存在
        Long bookId = Long.valueOf(dto.getBookId());
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

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

        // 转换为VO返回
        return toVO(borrow);
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

        // 获取书籍信息
        List<Long> bookIds = borrows.stream().map(BookBorrow::getBookId).distinct().collect(Collectors.toList());
        List<Book> books = bookMapper.selectList(Wrappers.<Book>lambdaQuery().in(Book::getId, bookIds));

        return borrows.stream().map(borrow -> {
            BookBorrowRecordVO vo = new BookBorrowRecordVO();
            vo.setId(borrow.getId());
            vo.setBookId(borrow.getBookId());
            vo.setUserId(borrow.getUserId());
            vo.setBorrowerName(borrow.getBorrowerName());
            vo.setBorrowTime(borrow.getBorrowTime());
            vo.setReturnTime(borrow.getReturnTime());
            vo.setBorrowType(borrow.getBorrowType());

            // 查找对应的书籍信息
            books.stream().filter(book -> book.getId().equals(borrow.getBookId())).findFirst().ifPresent(book -> {
                vo.setTitle(book.getTitle());
                vo.setCoverUrl(book.getCoverUrl());
            });

            return vo;
        }).collect(Collectors.toList());
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

        // 更新借书人姓名
        borrow.setBorrowerName(dto.getBorrowerName());

        // 更新借书时间（如果提供）
        if (dto.getBorrowTime() != null) {
            borrow.setBorrowTime(dto.getBorrowTime());
        }

        this.updateById(borrow);
    }

    private BookBorrowVO toVO(BookBorrow borrow) {
        BookBorrowVO vo = new BookBorrowVO();
        vo.setId(borrow.getId());
        vo.setBookId(borrow.getBookId());
        vo.setUserId(borrow.getUserId());
        vo.setBorrowerName(borrow.getBorrowerName());
        vo.setBorrowTime(borrow.getBorrowTime());
        vo.setReturnTime(borrow.getReturnTime());
        vo.setBorrowType(borrow.getBorrowType());
        vo.setBookName(borrow.getBookName());
        vo.setCoverUrl(borrow.getCoverUrl());
        return vo;
    }
}