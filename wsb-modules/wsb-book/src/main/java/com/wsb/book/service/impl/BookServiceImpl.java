package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookShelfDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.api.vo.MyBookListVO;
import com.wsb.book.api.vo.MyBookVO;
import com.wsb.book.api.vo.RecentBookVO;
import com.wsb.book.client.AliyunIsbnClient;
import com.wsb.book.client.GoogleBooksClient;
import com.wsb.book.client.RagFeignClient;
import com.wsb.book.convert.BookConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.domain.BookShelf;
import com.wsb.book.domain.Shelf;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookMapper;
import com.wsb.book.mapper.BookShelfMapper;
import com.wsb.book.mapper.ShelfMapper;
import com.wsb.book.response.AliyunIsbnResponse;
import com.wsb.book.response.AliyunIsbnResponse.BookDetail;
import com.wsb.book.response.GoogleBooksResponse;
import com.wsb.book.response.GoogleBooksResponse.VolumeInfo;
import com.wsb.book.service.BookService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final BookConverter bookConverter;
    private final BookBorrowMapper bookBorrowMapper;
    private final BookShelfMapper bookShelfMapper;
    private final ShelfMapper shelfMapper;
    private final AliyunIsbnClient aliyunIsbnClient;
    private final GoogleBooksClient googleBooksClient;
    private final RagFeignClient ragFeignClient;

    @Value("${aliyun.isbn.app-code}")
    private String aliyunAppCode;

    @Value("${google.books.api-key}")
    private String googleApiKey;

    @Override
    public Page<BookVO> searchByCondition(Integer page, Integer pageSize, String shelfId, String bookName, String classify) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Page<Book> pageParam = new Page<>(page, pageSize);

        if (StringUtils.isNotBlank(shelfId)) {
            try {
                Long shelfIdLong = Long.valueOf(shelfId);
                Shelf shelf = shelfMapper.selectById(shelfIdLong);
                if (shelf == null) {
                    throw new ServiceException("书架不存在");
                }
                if (!shelf.getUserId().equals(currentUserId)) {
                    throw new ServiceException("无权查询他人的书架");
                }

                List<Long> bookIds = bookShelfMapper.selectList(
                        Wrappers.<BookShelf>lambdaQuery()
                                .eq(BookShelf::getShelfId, shelfIdLong)
                                .select(BookShelf::getBookId)
                ).stream().map(BookShelf::getBookId).collect(Collectors.toList());

                if (bookIds.isEmpty()) {
                    Page<BookVO> emptyPage = new Page<>(page, pageSize);
                    emptyPage.setRecords(List.of());
                    emptyPage.setTotal(0);
                    return emptyPage;
                }

                LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(Book::getId, bookIds);
                if (StringUtils.isNotBlank(bookName)) {
                    wrapper.like(Book::getTitle, bookName);
                }
                if (StringUtils.isNotBlank(classify)) {
                    wrapper.eq(Book::getClassify, classify);
                }
                wrapper.orderByDesc(Book::getCreateTime);
                return bookConverter.toVOPage(this.page(pageParam, wrapper));
            } catch (NumberFormatException e) {
                throw new ServiceException("书架 ID 格式错误");
            }
        }

        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getUserId, currentUserId);
        if (StringUtils.isNotBlank(bookName)) {
            wrapper.like(Book::getTitle, bookName);
        }
        if (StringUtils.isNotBlank(classify)) {
            wrapper.eq(Book::getClassify, classify);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        return bookConverter.toVOPage(this.page(pageParam, wrapper));
    }

    @Override
    public MyBookListVO getMyBooks() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<Book> books = this.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, currentUserId)
                .orderByDesc(Book::getCreateTime));

        List<MyBookVO> bookVOs = books.stream()
                .map(bookConverter::toMyBookVO)
                .collect(Collectors.toList());

        MyBookListVO result = new MyBookListVO();
        result.setCount(books.size());
        result.setBooks(bookVOs);
        return result;
    }

    @Override
    public List<RecentBookVO> getRecentBooks() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Page<Book> pageParam = new Page<>(1, 6, false);
        LambdaQueryWrapper<Book> wrapper = Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, currentUserId)
                .orderByDesc(Book::getUpdateTime)
                .orderByDesc(Book::getCreateTime);

        return this.page(pageParam, wrapper)
                .getRecords()
                .stream()
                .map(bookConverter::toRecentBookVO)
                .collect(Collectors.toList());
    }

    @Override
    public IsbnBookVO getBookByIsbn(String isbn) {
        if (StringUtils.isBlank(isbn)) {
            throw new ServiceException("ISBN 不能为空");
        }

        IsbnBookVO aliyunBook = queryFromAliyun(isbn);
        IsbnBookVO googleBook = queryFromGoogle(isbn);
        IsbnBookVO mergedBook = mergeIsbnBook(aliyunBook, googleBook);
        if (mergedBook != null) {
            return mergedBook;
        }

        throw new ServiceException("未找到该 ISBN 对应的图书信息");
    }

    private IsbnBookVO queryFromAliyun(String isbn) {
        try {
            AliyunIsbnResponse response = aliyunIsbnClient.queryByIsbn(
                    AliyunIsbnClient.buildBody(isbn),
                    "APPCODE " + aliyunAppCode
            );

            if (response == null || response.getCode() == null || response.getCode() != 200) {
                log.debug("阿里云 ISBN API 返回失败: code={}", response != null ? response.getCode() : null);
                return null;
            }

            if (response.getData() == null || response.getData().getDetails() == null
                    || response.getData().getDetails().isEmpty()) {
                log.debug("阿里云 ISBN API 无图书数据");
                return null;
            }

            BookDetail detail = response.getData().getDetails().get(0);
            if (StringUtils.isBlank(detail.getTitle())) {
                return null;
            }

            return bookConverter.toIsbnBookVO(detail);
        } catch (Exception e) {
            log.warn("调用阿里云 ISBN API 失败: {}", e.getMessage());
            return null;
        }
    }

    private IsbnBookVO queryFromGoogle(String isbn) {
        try {
            String query = "isbn:" + isbn;
            GoogleBooksResponse response = googleBooksClient.searchByIsbn(query, googleApiKey);

            if (response == null || response.getTotalItems() == 0 || response.getItems() == null || response.getItems().isEmpty()) {
                return null;
            }

            GoogleBooksResponse.BookItem searchItem = response.getItems().get(0);
            VolumeInfo volumeInfo = searchItem.getVolumeInfo();
            if (StringUtils.isNotBlank(searchItem.getId())) {
                GoogleBooksResponse.BookItem detailItem = googleBooksClient.getVolumeById(searchItem.getId(), googleApiKey);
                if (detailItem != null && detailItem.getVolumeInfo() != null) {
                    volumeInfo = detailItem.getVolumeInfo();
                }
            }
            if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getTitle())) {
                return null;
            }

            return bookConverter.toIsbnBookVO(volumeInfo);
        } catch (Exception e) {
            log.warn("调用 Google Books API 失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookAddVO add(BookAddDTO dto) {
        Book book = bookConverter.toBook(dto);
        book.setUserId(StpUtil.getLoginIdAsLong());
        book.setIsDeleted(false);

        this.save(book);

        if (Boolean.TRUE.equals(dto.getIsBorrowed())) {
            BookBorrow borrow = new BookBorrow();
            borrow.setBookId(book.getId());
            borrow.setUserId(book.getUserId());
            borrow.setBorrowType(1);
            borrow.setBorrowerName(dto.getOwner());
            borrow.setBorrowTime(dto.getBorrowTime());
            borrow.setBookName(book.getTitle());
            borrow.setCoverUrl(book.getCoverUrl());
            borrow.setIsDeleted(false);
            bookBorrowMapper.insert(borrow);
        }

        Long shelfId = dto.getShelfId();
        if (shelfId != null) {
            Shelf shelf = shelfMapper.selectById(shelfId);
            if (shelf == null) {
                throw new ServiceException("指定的书架不存在");
            }
            if (!shelf.getUserId().equals(book.getUserId())) {
                throw new ServiceException("无权添加到该书架");
            }

            BookShelf relation = new BookShelf();
            relation.setBookId(book.getId());
            relation.setShelfId(shelfId);
            relation.setIsDeleted(false);
            bookShelfMapper.insert(relation);
        }

        enqueueSummaryAfterCommit(book.getId());
        return bookConverter.toBookAddVO(book);
    }

    @Override
    public BookVO detail(Long bookId) {
        Book book = this.getById(bookId);
        return bookConverter.toBookVO(book);
    }

    @Override
    public BookVO update(BookUpdateDTO dto) {
        Book book = this.getById(dto.getId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!book.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权修改他人书籍");
        }

        if (dto.getTitle() != null && StringUtils.isBlank(dto.getTitle())) {
            throw new ServiceException("书名不能为空");
        }

        bookConverter.updateBookFromDto(dto, book);
        this.updateById(book);
        return bookConverter.toBookVO(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Book book = this.getById(id);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!book.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权删除他人书籍");
        }

        Long count = bookBorrowMapper.selectCount(
                Wrappers.<BookBorrow>lambdaQuery()
                        .eq(BookBorrow::getBookId, id)
                        .eq(BookBorrow::getBorrowType, 2)
                        .isNull(BookBorrow::getReturnTime));

        if (count > 0) {
            throw new ServiceException("书籍已借出且未归还，无法删除");
        }

        bookBorrowMapper.delete(
                Wrappers.<BookBorrow>lambdaQuery()
                        .eq(BookBorrow::getBookId, id));

        bookShelfMapper.delete(
                Wrappers.<BookShelf>lambdaQuery()
                        .eq(BookShelf::getBookId, id));

        this.removeById(id);
    }

    @Override
    public void onShelf(BookShelfDTO dto) {
        Book book = this.getById(dto.getBookId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        Shelf shelf = shelfMapper.selectById(dto.getShelfId());
        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }
        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权将书籍放入他人书架");
        }

        Long count = bookShelfMapper.selectCount(new LambdaQueryWrapper<BookShelf>()
                .eq(BookShelf::getShelfId, dto.getShelfId())
                .eq(BookShelf::getBookId, dto.getBookId()));
        if (count > 0) {
            throw new ServiceException("书籍已存在于该书架");
        }

        BookShelf bookShelf = new BookShelf();
        bookShelf.setBookId(book.getId());
        bookShelf.setShelfId(shelf.getId());
        bookShelf.setIsDeleted(false);
        bookShelfMapper.insert(bookShelf);
    }

    @Override
    public void offShelf(BookShelfDTO dto) {
        Book book = this.getById(dto.getBookId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        Shelf shelf = shelfMapper.selectById(dto.getShelfId());
        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }
        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权操作他人书架");
        }

        int rows = bookShelfMapper.delete(new LambdaQueryWrapper<BookShelf>()
                .eq(BookShelf::getShelfId, dto.getShelfId())
                .eq(BookShelf::getBookId, dto.getBookId()));
        if (rows == 0) {
            throw new ServiceException("书架中未找到该书籍");
        }
    }

    private IsbnBookVO mergeIsbnBook(IsbnBookVO primary, IsbnBookVO fallback) {
        if (primary == null) {
            if (fallback != null) fallback.setSummary(null);
            return fallback;
        }
        if (fallback == null) {
            primary.setSummary(null);
            return primary;
        }

        primary.setTitle(firstNonBlank(primary.getTitle(), fallback.getTitle()));
        primary.setAuthor(resolvePreferredAuthor(primary.getAuthor(), fallback.getAuthor()));
        primary.setSubtitle(firstNonBlank(primary.getSubtitle(), fallback.getSubtitle()));
        primary.setPublishDate(firstNonBlank(primary.getPublishDate(), fallback.getPublishDate()));
        primary.setPublisher(firstNonBlank(primary.getPublisher(), fallback.getPublisher()));
        primary.setPageCount(firstNonBlank(primary.getPageCount(), fallback.getPageCount()));
        primary.setPrice(firstNonBlank(primary.getPrice(), fallback.getPrice()));
        primary.setCoverUrl(firstNonBlank(primary.getCoverUrl(), fallback.getCoverUrl()));
        primary.setIsbn(firstNonBlank(primary.getIsbn(), fallback.getIsbn()));
        primary.setIsbn10(firstNonBlank(primary.getIsbn10(), fallback.getIsbn10()));
        primary.setPubPlace(firstNonBlank(primary.getPubPlace(), fallback.getPubPlace()));
        primary.setBinding(firstNonBlank(primary.getBinding(), fallback.getBinding()));
        primary.setKeyword(firstNonBlank(primary.getKeyword(), fallback.getKeyword()));
        primary.setCip(firstNonBlank(primary.getCip(), fallback.getCip()));
        primary.setEdition(firstNonBlank(primary.getEdition(), fallback.getEdition()));
        primary.setImpression(firstNonBlank(primary.getImpression(), fallback.getImpression()));
        primary.setLanguage(firstNonBlank(primary.getLanguage(), fallback.getLanguage()));
        primary.setBookFormat(firstNonBlank(primary.getBookFormat(), fallback.getBookFormat()));
        primary.setClc(firstNonBlank(primary.getClc(), fallback.getClc()));
        primary.setSummary(null);
        return primary;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.isNotBlank(primary) ? primary : fallback;
    }

    private String resolvePreferredAuthor(String primary, String fallback) {
        if (isPlaceholderAuthor(primary) && StringUtils.isNotBlank(fallback)) {
            return fallback;
        }
        return firstNonBlank(primary, fallback);
    }

    private boolean isPlaceholderAuthor(String author) {
        if (StringUtils.isBlank(author)) {
            return false;
        }
        String normalized = author.replaceAll("[\\s,，/]+", "");
        return normalized.contains("无名氏")
                || normalized.contains("佚名")
                || normalized.contains("匿名")
                || normalized.contains("不详");
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
}
