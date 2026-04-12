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
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.client.RagFeignClient;
import com.wsb.book.convert.BookBorrowConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.domain.BookShelf;
import com.wsb.book.domain.Shelf;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.mapper.BookShelfMapper;
import com.wsb.book.mapper.ShelfMapper;
import com.wsb.book.service.BookBorrowService;
import com.wsb.book.service.BookService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书借阅服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {

    private static final int BORROW_TYPE_IN = 1;
    private static final int BORROW_TYPE_OUT = 2;

    private final BookMapper bookMapper;
    private final BookShelfMapper bookShelfMapper;
    private final ShelfMapper shelfMapper;
    private final BookService bookService;
    private final BookBorrowConverter bookBorrowConverter;
    private final RagFeignClient ragFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookBorrowVO borrow(BookBorrowDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        validateBorrowDates(dto.getBorrowTime(), dto.getDueTime());

        if (dto.getBorrowType() == null) {
            throw new ServiceException("借阅类型不能为空");
        }
        if (dto.getBorrowType() == BORROW_TYPE_IN) {
            return borrowOfflineBook(dto, currentUserId);
        }
        if (dto.getBorrowType() == BORROW_TYPE_OUT) {
            return borrowOwnedBook(dto, currentUserId);
        }

        throw new ServiceException("借阅类型不正确");
    }

    private BookBorrowVO borrowOwnedBook(BookBorrowDTO dto, Long currentUserId) {
        if (dto.getBookId() == null) {
            throw new ServiceException("借出时必须选择图书");
        }

        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new ServiceException("图书不存在");
        }
        if (!currentUserId.equals(book.getUserId())) {
            throw new ServiceException("只能借出自己的图书");
        }
        if (Boolean.TRUE.equals(book.getIsBorrowed())) {
            throw new ServiceException("借入的图书不能再次借出");
        }

        validateActiveBorrow(book.getId());
        BookBorrow borrow = createBorrowRecord(dto, currentUserId, book);
        this.save(borrow);
        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    private BookBorrowVO borrowOfflineBook(BookBorrowDTO dto, Long currentUserId) {
        Book book = createOfflineBorrowedBook(dto, currentUserId);
        BookBorrow borrow = createBorrowRecord(dto, currentUserId, book);
        this.save(borrow);
        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    private BookBorrow createBorrowRecord(BookBorrowDTO dto, Long currentUserId, Book book) {
        BookBorrow borrow = bookBorrowConverter.toBookBorrow(dto);
        borrow.setBookId(book.getId());
        borrow.setUserId(currentUserId);
        borrow.setBookName(book.getTitle());
        borrow.setCoverUrl(book.getCoverUrl());
        borrow.setStatus(resolveBorrowStatus(dto.getDueTime(), null));
        borrow.setIsDeleted(false);
        return borrow;
    }

    private Book createOfflineBorrowedBook(BookBorrowDTO dto, Long currentUserId) {
        Book book = new Book();
        String isbn = StringUtils.trimToNull(dto.getIsbn());

        if (isbn != null) {
            Book existingBook = findBookMetadataByIsbn(isbn);
            if (existingBook != null) {
                applyBookMetadata(book, existingBook);
            } else {
                applyIsbnMetadata(book, queryIsbnMetadata(isbn));
            }
        }
        applyManualBookMetadata(book, dto);

        if (StringUtils.isBlank(book.getTitle())) {
            throw new ServiceException("线下借入时请填写书名，或先填写 ISBN 获取图书信息");
        }

        book.setUserId(currentUserId);
        book.setIsDeleted(false);
        book.setIsBorrowed(true);
        book.setIsOnShelf(dto.getShelfId() != null);
        book.setEmbeddingStatus(0);
        bookMapper.insert(book);

        if (dto.getShelfId() != null) {
            attachToShelf(book.getId(), dto.getShelfId(), currentUserId);
        }

        enqueueSummaryAfterCommit(book.getId());
        return book;
    }

    private Book findBookMetadataByIsbn(String isbn) {
        String compactIsbn = isbn.replaceAll("[\\s-]", "");
        return bookMapper.selectOne(Wrappers.<Book>lambdaQuery()
                .eq(Book::getIsDeleted, false)
                .and(wrapper -> {
                    wrapper.eq(Book::getIsbn, isbn)
                            .or()
                            .eq(Book::getIsbn10, isbn);
                    if (!compactIsbn.equals(isbn)) {
                        wrapper.or()
                                .eq(Book::getIsbn, compactIsbn)
                                .or()
                                .eq(Book::getIsbn10, compactIsbn);
                    }
                })
                .orderByDesc(Book::getUpdateTime)
                .orderByDesc(Book::getCreateTime)
                .last("LIMIT 1"));
    }

    private IsbnBookVO queryIsbnMetadata(String isbn) {
        try {
            return bookService.getBookByIsbn(isbn);
        } catch (Exception e) {
            log.warn("线下借入 ISBN 元数据查询失败: isbn={}", isbn, e);
            return null;
        }
    }

    private void applyBookMetadata(Book target, Book source) {
        if (source == null) {
            return;
        }
        target.setTitle(source.getTitle());
        target.setSubtitle(source.getSubtitle());
        target.setAuthor(source.getAuthor());
        target.setSummary(source.getSummary());
        target.setPublisher(source.getPublisher());
        target.setPublishDate(source.getPublishDate());
        target.setPageCount(source.getPageCount());
        target.setPrice(source.getPrice());
        target.setBinding(source.getBinding());
        target.setIsbn(source.getIsbn());
        target.setIsbn10(source.getIsbn10());
        target.setKeyword(source.getKeyword());
        target.setCoverUrl(source.getCoverUrl());
        target.setLanguage(source.getLanguage());
        target.setBookFormat(source.getBookFormat());
        target.setClc(source.getClc());
        target.setCip(source.getCip());
        target.setEdition(source.getEdition());
        target.setImpression(source.getImpression());
    }

    private void applyIsbnMetadata(Book target, IsbnBookVO source) {
        if (source == null) {
            return;
        }
        target.setTitle(source.getTitle());
        target.setSubtitle(source.getSubtitle());
        target.setAuthor(source.getAuthor());
        target.setSummary(source.getSummary());
        target.setPublisher(source.getPublisher());
        target.setPublishDate(parsePublishDate(source.getPublishDate()));
        target.setPageCount(parseInteger(source.getPageCount()));
        target.setPrice(parseDouble(source.getPrice()));
        target.setBinding(source.getBinding());
        target.setIsbn(source.getIsbn());
        target.setIsbn10(source.getIsbn10());
        target.setKeyword(source.getKeyword());
        target.setCoverUrl(source.getCoverUrl());
        target.setLanguage(source.getLanguage());
        target.setBookFormat(source.getBookFormat());
        target.setClc(source.getClc());
        target.setCip(source.getCip());
        target.setEdition(source.getEdition());
        target.setImpression(source.getImpression());
    }

    private void applyManualBookMetadata(Book target, BookBorrowDTO dto) {
        setIfNotBlank(target::setTitle, dto.getTitle());
        setIfNotBlank(target::setSubtitle, dto.getSubtitle());
        setIfNotBlank(target::setAuthor, dto.getAuthor());
        setIfNotBlank(target::setPublisher, dto.getPublisher());
        if (dto.getPublishDate() != null) {
            target.setPublishDate(dto.getPublishDate());
        }
        if (dto.getPageCount() != null) {
            target.setPageCount(dto.getPageCount());
        }
        if (dto.getPrice() != null) {
            target.setPrice(dto.getPrice());
        }
        setIfNotBlank(target::setBinding, dto.getBinding());
        setIfNotBlank(target::setIsbn, dto.getIsbn());
        setIfNotBlank(target::setIsbn10, dto.getIsbn10());
        setIfNotBlank(target::setKeyword, dto.getKeyword());
        setIfNotBlank(target::setCoverUrl, dto.getCoverUrl());
    }

    private void setIfNotBlank(java.util.function.Consumer<String> setter, String value) {
        if (StringUtils.isNotBlank(value)) {
            setter.accept(value.trim());
        }
    }

    private LocalDate parsePublishDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String normalized = value.trim()
                .replace('，', '-')
                .replace(',', '-')
                .replace('/', '-')
                .replace('－', '-')
                .replace('年', '-')
                .replace('月', '-')
                .replace("日", "")
                .replaceAll("-+$", "");
        try {
            if (normalized.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(normalized), 1, 1);
            }
            if (normalized.matches("\\d{4}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
            }
            if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                return LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            }
        } catch (Exception ignored) {
            log.debug("ISBN 出版日期解析失败: {}", value);
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9.]", "");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        try {
            return Double.valueOf(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void attachToShelf(Long bookId, Long shelfId, Long currentUserId) {
        Shelf shelf = shelfMapper.selectById(shelfId);
        if (shelf == null) {
            throw new ServiceException("指定的书架不存在");
        }
        if (!currentUserId.equals(shelf.getUserId())) {
            throw new ServiceException("无权添加到该书架");
        }

        BookShelf relation = new BookShelf();
        relation.setBookId(bookId);
        relation.setShelfId(shelfId);
        relation.setIsDeleted(false);
        bookShelfMapper.insert(relation);
    }

    private void enqueueSummaryAfterCommit(Long bookId) {
        if (bookId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueueSummary(bookId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueueSummary(bookId);
            }
        });
    }

    private void enqueueSummary(Long bookId) {
        try {
            ragFeignClient.enqueueSummary(bookId);
            log.info("已通过 RAG 内部接口加入摘要任务: bookId={}", bookId);
        } catch (Exception e) {
            log.warn("通知 RAG 加入摘要任务失败: bookId={}", bookId, e);
        }
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
        vo.setBorrowedIn((int) borrows.stream().filter(item -> item.getBorrowType() != null && item.getBorrowType() == BORROW_TYPE_IN).count());
        vo.setBorrowedOut((int) borrows.stream().filter(item -> item.getBorrowType() != null && item.getBorrowType() == BORROW_TYPE_OUT).count());
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
