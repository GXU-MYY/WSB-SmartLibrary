package com.wsb.book.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.constant.BookBorrowStatus;
import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.book.convert.BookInnerConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.service.BookInnerService;
import com.wsb.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书内部服务实现
 */
@Service
@RequiredArgsConstructor
public class BookInnerServiceImpl implements BookInnerService {

    private final BookService bookService;
    private final BookBorrowMapper bookBorrowMapper;
    private final BookInnerConverter bookInnerConverter;

    @Override
    public BookRemoteDTO getBookById(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            return null;
        }
        return bookInnerConverter.toBookRemoteDTO(book);
    }

    @Override
    public List<BookRemoteDTO> getBooksByIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        return bookInnerConverter.toBookRemoteDTOList(bookService.listByIds(bookIds));
    }

    @Override
    public List<UserBookCountDTO> countBooksByUsers(List<Long> userIds) {
        List<Book> books;
        if (userIds != null && !userIds.isEmpty()) {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .in(Book::getUserId, userIds)
                    .eq(Book::getIsDeleted, false));
        } else {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .eq(Book::getIsDeleted, false));
        }

        return books.stream()
                .collect(Collectors.groupingBy(Book::getUserId, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    UserBookCountDTO dto = new UserBookCountDTO();
                    dto.setUserId(entry.getKey());
                    dto.setBookCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryCountDTO> countBooksByCategory(Long userId) {
        List<Book> books = bookService.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, userId)
                .eq(Book::getIsDeleted, false));

        return books.stream()
                .filter(book -> book.getClassify() != null && !book.getClassify().isEmpty())
                .collect(Collectors.groupingBy(Book::getClassify, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    CategoryCountDTO dto = new CategoryCountDTO();
                    dto.setCategory(entry.getKey());
                    dto.setCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getBookIdsByOwner(Long userId) {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getUserId, userId)
                        .eq(Book::getIsDeleted, false)
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookBorrowCountDTO> countBorrowByBooks(List<Long> bookIds) {
        syncOverdueBorrows();
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2));
        }

        return borrows.stream()
                .collect(Collectors.groupingBy(BookBorrow::getBookId, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    BookBorrowCountDTO dto = new BookBorrowCountDTO();
                    dto.setBookId(entry.getKey());
                    dto.setBorrowCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserBorrowStatsDTO getUserBorrowStats(Long userId) {
        syncOverdueBorrows();
        List<BookBorrow> borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, userId)
                .eq(BookBorrow::getIsDeleted, false));

        UserBorrowStatsDTO dto = new UserBorrowStatsDTO();
        dto.setTotalBorrowed(borrows.size());
        dto.setUnreturned((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        return dto;
    }

    @Override
    public Integer countUnreturnedByOwner(Long ownerId) {
        syncOverdueBorrows();
        Long count = bookBorrowMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, ownerId)
                .eq(BookBorrow::getBorrowType, 2)
                .eq(BookBorrow::getIsDeleted, false)
                .in(BookBorrow::getStatus, BookBorrowStatus.BORROWING, BookBorrowStatus.OVERDUE));
        return count.intValue();
    }

    @Override
    public List<BorrowCategoryStatsDTO> getBorrowStatsByCategory(List<Long> bookIds) {
        return List.of();
    }

    @Override
    public BorrowCategoryStatsDTO getBorrowSummary(List<Long> bookIds) {
        syncOverdueBorrows();
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false));
        }

        BorrowCategoryStatsDTO dto = new BorrowCategoryStatsDTO();
        dto.setTotal(borrows.size());
        dto.setReading((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        dto.setRead((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() == BookBorrowStatus.RETURNED)
                .count());
        return dto;
    }

    @Override
    public List<Long> getBooksWithNullSummary() {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getIsDeleted, false)
                        .and(wrapper -> wrapper.isNull(Book::getSummary).or().eq(Book::getSummary, ""))
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getBooksPendingEmbedding() {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getIsDeleted, false)
                        .and(wrapper -> wrapper.isNull(Book::getEmbeddingStatus).or().eq(Book::getEmbeddingStatus, 0))
                        .isNotNull(Book::getSummary)
                        .ne(Book::getSummary, "")
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void updateSummary(Long bookId, String summary) {
        Book book = new Book();
        book.setId(bookId);
        book.setSummary(summary);
        bookService.updateById(book);
    }

    @Override
    public void updateEmbeddingStatus(Long bookId, Integer status) {
        Book book = new Book();
        book.setId(bookId);
        book.setEmbeddingStatus(status);
        bookService.updateById(book);
    }

    private void syncOverdueBorrows() {
        LocalDate today = LocalDate.now();

        bookBorrowMapper.update(null, Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getIsDeleted, false)
                .eq(BookBorrow::getStatus, BookBorrowStatus.BORROWING)
                .isNotNull(BookBorrow::getDueTime)
                .lt(BookBorrow::getDueTime, today)
                .set(BookBorrow::getStatus, BookBorrowStatus.OVERDUE));

        bookBorrowMapper.update(null, Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getIsDeleted, false)
                .eq(BookBorrow::getStatus, BookBorrowStatus.OVERDUE)
                .and(wrapper -> wrapper
                        .isNull(BookBorrow::getDueTime)
                        .or()
                        .ge(BookBorrow::getDueTime, today))
                .set(BookBorrow::getStatus, BookBorrowStatus.BORROWING));
    }
}
