package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.constant.BookBorrowStatus;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.dto.CommunityBorrowCreateDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowSummaryVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.api.vo.CommunityBorrowFlowVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.client.RagFeignClient;
import com.wsb.book.convert.BookBorrowConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.domain.BookShelf;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.mapper.BookShelfMapper;
import com.wsb.book.service.BookBorrowService;
import com.wsb.book.service.BookService;
import com.wsb.book.service.support.BookRemoteCacheService;
import com.wsb.book.service.support.CommunityStatisticsCacheEvictService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookBorrowServiceImpl extends ServiceImpl<BookBorrowMapper, BookBorrow> implements BookBorrowService {

    private static final int BORROW_TYPE_IN = 1;
    private static final int BORROW_TYPE_OUT = 2;

    private final BookMapper bookMapper;
    private final BookShelfMapper bookShelfMapper;
    private final BookService bookService;
    private final BookBorrowConverter bookBorrowConverter;
    private final RagFeignClient ragFeignClient;
    private final BookRemoteCacheService bookRemoteCacheService;
    private final CommunityStatisticsCacheEvictService communityStatisticsCacheEvictService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityBorrowFlowVO createCommunityBorrowFlow(CommunityBorrowCreateDTO dto) {
        if (dto == null || dto.getBookId() == null) {
            throw new ServiceException("群组借阅请求缺少图书信息");
        }
        if (dto.getOwnerUserId() == null || dto.getBorrowerUserId() == null) {
            throw new ServiceException("群组借阅用户信息不完整");
        }
        if (dto.getOwnerUserId().equals(dto.getBorrowerUserId())) {
            throw new ServiceException("出借人与借入人不能是同一用户");
        }
        validateBorrowDates(dto.getBorrowTime(), dto.getDueTime());

        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
            throw new ServiceException("图书不存在");
        }
        if (!dto.getOwnerUserId().equals(book.getUserId())) {
            throw new ServiceException("该图书不属于当前出借人");
        }
        if (Boolean.TRUE.equals(book.getIsBorrowed())) {
            throw new ServiceException("借入图书不能发起群组借出");
        }

        validateActiveBorrow(book.getId());
        Book borrowedBook = createCommunityBorrowedBook(book, dto.getBorrowerUserId());

        String flowId = UUID.randomUUID().toString();
        BookBorrow inBorrow = buildCommunityBorrowRecord(
                borrowedBook,
                dto.getBorrowerUserId(),
                dto.getOwnerNickname(),
                BORROW_TYPE_IN,
                dto.getBorrowTime(),
                dto.getDueTime(),
                flowId,
                dto.getGroupId(),
                dto.getRequestId()
        );
        BookBorrow outBorrow = buildCommunityBorrowRecord(
                book,
                dto.getOwnerUserId(),
                dto.getBorrowerNickname(),
                BORROW_TYPE_OUT,
                dto.getBorrowTime(),
                dto.getDueTime(),
                flowId,
                dto.getGroupId(),
                dto.getRequestId()
        );

        this.save(inBorrow);
        this.save(outBorrow);
        refreshBookLentOutStatus(book.getId());
        evictBookCachesAfterCommit(List.of(book.getId(), borrowedBook.getId()));
        evictPersonalStatsAfterCommit(List.of(dto.getOwnerUserId(), dto.getBorrowerUserId()));
        evictUserRankAfterCommit();

        CommunityBorrowFlowVO vo = new CommunityBorrowFlowVO();
        vo.setBorrowFlowId(flowId);
        vo.setInBorrowId(inBorrow.getId());
        vo.setOutBorrowId(outBorrow.getId());
        return vo;
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
        refreshBookLentOutStatus(book.getId());
        evictBookCachesAfterCommit(List.of(book.getId()));
        evictPersonalStatsAfterCommit(List.of(currentUserId));
        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    private BookBorrowVO borrowOfflineBook(BookBorrowDTO dto, Long currentUserId) {
        Book book = createOfflineBorrowedBook(dto, currentUserId);
        BookBorrow borrow = createBorrowRecord(dto, currentUserId, book);
        this.save(borrow);
        evictBookCachesAfterCommit(List.of(book.getId()));
        evictPersonalStatsAfterCommit(List.of(currentUserId));
        evictUserRankAfterCommit();
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

    private BookBorrow buildCommunityBorrowRecord(
            Book book,
            Long userId,
            String borrowerName,
            Integer borrowType,
            LocalDate borrowTime,
            LocalDate dueTime,
            String flowId,
            Long groupId,
            Long requestId) {
        BookBorrow borrow = new BookBorrow();
        borrow.setBookId(book.getId());
        borrow.setUserId(userId);
        borrow.setBorrowerName(StringUtils.defaultIfBlank(borrowerName, "未命名读者"));
        borrow.setBorrowTime(borrowTime);
        borrow.setDueTime(dueTime);
        borrow.setBorrowType(borrowType);
        borrow.setStatus(resolveBorrowStatus(dueTime, null));
        borrow.setBookName(book.getTitle());
        borrow.setCoverUrl(book.getCoverUrl());
        borrow.setBorrowFlowId(flowId);
        borrow.setGroupId(groupId);
        borrow.setRequestId(requestId);
        borrow.setIsDeleted(false);
        return borrow;
    }

    private Book createOfflineBorrowedBook(BookBorrowDTO dto, Long currentUserId) {
        if (dto.getShelfId() != null) {
            throw new ServiceException("借入图书不能上架");
        }

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
            throw new ServiceException("线下借入时请填写书名，或先通过 ISBN 获取图书信息");
        }

        book.setUserId(currentUserId);
        book.setIsDeleted(false);
        book.setIsBorrowed(true);
        book.setIsLentOut(false);
        book.setIsOnShelf(false);
        book.setEmbeddingStatus(0);
        bookMapper.insert(book);

        enqueueSummaryAfterCommit(book.getId());
        return book;
    }

    private Book createCommunityBorrowedBook(Book source, Long borrowerUserId) {
        Book book = new Book();
        applyBookMetadata(book, source);
        book.setClassify(source.getClassify());
        book.setLabel(source.getLabel());
        book.setRemark(source.getRemark());
        book.setUserId(borrowerUserId);
        book.setIsDeleted(false);
        book.setIsBorrowed(true);
        book.setIsLentOut(false);
        book.setIsOnShelf(false);
        book.setEmbeddingStatus(source.getEmbeddingStatus());
        bookMapper.insert(book);
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
                .replace('．', '-')
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

    private void enqueueSummaryAfterCommit(Long bookId) {
        if (bookId == null) {
            return;
        }
        runAfterCommit(() -> enqueueSummary(bookId));
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

        if (borrow.getGroupId() != null && (borrow.getBorrowType() == null || borrow.getBorrowType() != BORROW_TYPE_IN)) {
            throw new ServiceException("群组借阅记录仅借入方可归还");
        }

        List<Long> relatedBookIds = getRelatedBookIds(borrow);
        List<Long> relatedUserIds = getRelatedUserIds(borrow);

        borrow.setReturnTime(dto.getReturnTime());
        borrow.setStatus(BookBorrowStatus.RETURNED);
        this.updateById(borrow);
        syncFlowReturnIfNeeded(borrow);
        cleanupReturnedBorrowedBooks(borrow);
        refreshRelatedBookStates(relatedBookIds);
        evictBookCachesAfterCommit(relatedBookIds);
        evictPersonalStatsAfterCommit(relatedUserIds);
        evictUserRankAfterCommit();

        return bookBorrowConverter.toBookBorrowVO(borrow);
    }

    @Override
    public Page<BookBorrowRecordVO> getRecords(Integer page, Integer pageSize, Integer borrowType, Integer status) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

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

        if (borrow.getGroupId() != null) {
            throw new ServiceException("群组借阅记录不支持编辑");
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
        syncFlowUpdateIfNeeded(borrow);
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

    private void syncFlowReturnIfNeeded(BookBorrow borrow) {
        if (borrow == null || StringUtils.isBlank(borrow.getBorrowFlowId())) {
            return;
        }

        this.update(Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getBorrowFlowId, borrow.getBorrowFlowId())
                .ne(BookBorrow::getId, borrow.getId())
                .eq(BookBorrow::getIsDeleted, false)
                .set(BookBorrow::getReturnTime, borrow.getReturnTime())
                .set(BookBorrow::getStatus, BookBorrowStatus.RETURNED));
    }

    private void cleanupReturnedBorrowedBooks(BookBorrow borrow) {
        if (borrow == null) {
            return;
        }

        List<BookBorrow> relatedBorrows = StringUtils.isBlank(borrow.getBorrowFlowId())
                ? List.of(borrow)
                : this.list(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBorrowFlowId, borrow.getBorrowFlowId())
                .eq(BookBorrow::getIsDeleted, false));

        relatedBorrows.stream()
                .filter(item -> item.getBorrowType() != null && item.getBorrowType() == BORROW_TYPE_IN)
                .map(BookBorrow::getBookId)
                .distinct()
                .forEach(this::deleteBorrowedBookIfInactive);
    }

    private void deleteBorrowedBookIfInactive(Long bookId) {
        if (bookId == null) {
            return;
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted()) || !Boolean.TRUE.equals(book.getIsBorrowed())) {
            return;
        }

        Long activeCount = this.baseMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, bookId)
                .eq(BookBorrow::getIsDeleted, false)
                .in(BookBorrow::getStatus, BookBorrowStatus.BORROWING, BookBorrowStatus.OVERDUE));
        if (activeCount != null && activeCount > 0) {
            return;
        }

        bookShelfMapper.delete(Wrappers.<BookShelf>lambdaQuery()
                .eq(BookShelf::getBookId, bookId));
        bookMapper.deleteById(bookId);
    }

    private void syncFlowUpdateIfNeeded(BookBorrow borrow) {
        if (borrow == null || StringUtils.isBlank(borrow.getBorrowFlowId())) {
            return;
        }

        int syncedStatus = borrow.getReturnTime() != null
                ? BookBorrowStatus.RETURNED
                : resolveBorrowStatus(borrow.getDueTime(), null);

        this.update(Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getBorrowFlowId, borrow.getBorrowFlowId())
                .ne(BookBorrow::getId, borrow.getId())
                .eq(BookBorrow::getIsDeleted, false)
                .set(BookBorrow::getBorrowTime, borrow.getBorrowTime())
                .set(BookBorrow::getDueTime, borrow.getDueTime())
                .set(borrow.getReturnTime() != null, BookBorrow::getReturnTime, borrow.getReturnTime())
                .set(BookBorrow::getStatus, syncedStatus));
    }

    private List<Long> getRelatedBookIds(BookBorrow borrow) {
        if (borrow == null) {
            return List.of();
        }

        return StringUtils.isBlank(borrow.getBorrowFlowId())
                ? List.of(borrow.getBookId())
                : this.list(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBorrowFlowId, borrow.getBorrowFlowId())
                .eq(BookBorrow::getIsDeleted, false))
                .stream()
                .map(BookBorrow::getBookId)
                .distinct()
                .toList();
    }

    private List<Long> getRelatedUserIds(BookBorrow borrow) {
        if (borrow == null) {
            return List.of();
        }

        return StringUtils.isBlank(borrow.getBorrowFlowId())
                ? List.of(borrow.getUserId())
                : this.list(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBorrowFlowId, borrow.getBorrowFlowId())
                .eq(BookBorrow::getIsDeleted, false))
                .stream()
                .map(BookBorrow::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void refreshRelatedBookStates(List<Long> relatedBookIds) {
        relatedBookIds.stream()
                .filter(Objects::nonNull)
                .forEach(this::refreshBookLentOutStatus);
    }

    private void refreshBookLentOutStatus(Long bookId) {
        if (bookId == null) {
            return;
        }

        Long activeOutCount = this.baseMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, bookId)
                .eq(BookBorrow::getBorrowType, BORROW_TYPE_OUT)
                .eq(BookBorrow::getIsDeleted, false)
                .in(BookBorrow::getStatus, BookBorrowStatus.BORROWING, BookBorrowStatus.OVERDUE));

        Book update = new Book();
        update.setId(bookId);
        update.setIsLentOut(activeOutCount != null && activeOutCount > 0);
        bookMapper.updateById(update);
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

    private void evictBookCachesAfterCommit(Collection<Long> bookIds) {
        List<Long> validBookIds = bookIds == null ? List.of() : bookIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (validBookIds.isEmpty()) {
            return;
        }

        runAfterCommit(() -> bookRemoteCacheService.evictBooks(validBookIds));
    }

    private void evictPersonalStatsAfterCommit(Collection<Long> userIds) {
        List<Long> validUserIds = userIds == null ? List.of() : userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (validUserIds.isEmpty()) {
            return;
        }

        runAfterCommit(() -> communityStatisticsCacheEvictService.evictPersonalStats(validUserIds));
    }

    private void evictUserRankAfterCommit() {
        runAfterCommit(communityStatisticsCacheEvictService::evictUserRank);
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
