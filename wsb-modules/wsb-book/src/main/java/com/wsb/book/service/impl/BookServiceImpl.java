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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书服务实现类
 */
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

    @Value("${aliyun.isbn.app-code}")
    private String aliyunAppCode;

    @Value("${google.books.api-key}")
    private String googleApiKey;

    @Override
    public Page<BookVO> searchByCondition(Integer page, Integer pageSize, String shelfId, String bookName, String classify) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Page<Book> pageParam = new Page<>(page, pageSize);

        // 如果指定了书架，先校验书架是否存在且属于当前用户
        if (StringUtils.isNotBlank(shelfId)) {
            try {
                Long shelfIdLong = Long.valueOf(shelfId);
                Shelf shelf = shelfMapper.selectById(shelfIdLong);
                if (shelf == null) {
                    throw new ServiceException("书架不存在");
                }
                if (!shelf.getUserId().equals(currentUserId)) {
                    throw new ServiceException("无权限查询他人书架");
                }
                // 查询该书架下的书籍ID列表
                List<Long> bookIds = bookShelfMapper.selectList(
                        Wrappers.<BookShelf>lambdaQuery()
                                .eq(BookShelf::getShelfId, shelfIdLong)
                                .select(BookShelf::getBookId)
                ).stream().map(BookShelf::getBookId).collect(Collectors.toList());

                // 如果书架下没有书籍，返回空结果
                if (bookIds.isEmpty()) {
                    Page<BookVO> emptyPage = new Page<>(page, pageSize);
                    emptyPage.setRecords(List.of());
                    emptyPage.setTotal(0);
                    return emptyPage;
                }

                // 按书籍ID列表、书名、分类查询
                LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(Book::getId, bookIds);
                if (StringUtils.isNotBlank(bookName)) {
                    wrapper.like(Book::getTitle, bookName);
                }
                if (StringUtils.isNotBlank(classify)) {
                    wrapper.eq(Book::getClassify, classify);
                }
                wrapper.orderByDesc(Book::getCreateTime);
                Page<Book> bookPage = this.page(pageParam, wrapper);
                return bookConverter.toVOPage(bookPage);
            } catch (NumberFormatException e) {
                throw new ServiceException("书架ID格式错误");
            }
        } else {
            // 未指定书架，查询用户所有书籍
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getUserId, currentUserId);
            if (StringUtils.isNotBlank(bookName)) {
                wrapper.like(Book::getTitle, bookName);
            }
            if (StringUtils.isNotBlank(classify)) {
                wrapper.eq(Book::getClassify, classify);
            }
            wrapper.orderByDesc(Book::getCreateTime);
            Page<Book> bookPage = this.page(pageParam, wrapper);
            return bookConverter.toVOPage(bookPage);
        }
    }

    @Override
    public MyBookListVO getMyBooks() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询当前用户的所有书籍
        List<Book> books = this.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, currentUserId)
                .orderByDesc(Book::getCreateTime));

        // 转换为VO
        List<MyBookVO> bookVOs = books.stream().map(bookConverter::toMyBookVO).collect(Collectors.toList());

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
            throw new ServiceException("ISBN不能为空");
        }

        // 1. 优先调用阿里云 ISBN API
        IsbnBookVO vo = queryFromAliyun(isbn);
        if (vo != null) {
            return vo;
        }

        // 2. 阿里云无结果，调用 Google Books API
        vo = queryFromGoogle(isbn);
        if (vo != null) {
            return vo;
        }

        // 两个 API 都无结果
        throw new ServiceException("未找到该ISBN对应的图书信息");
    }

    /**
     * 从阿里云 ISBN API 查询图书信息
     */
    private IsbnBookVO queryFromAliyun(String isbn) {
        try {
            AliyunIsbnResponse response = aliyunIsbnClient.queryByIsbn(
                    AliyunIsbnClient.buildBody(isbn),
                    "APPCODE " + aliyunAppCode
            );

            // 检查响应
            if (response == null || response.getCode() == null || response.getCode() != 200) {
                log.debug("阿里云 ISBN API 返回失败: code={}", response != null ? response.getCode() : null);
                return null;
            }

            if (response.getData() == null || response.getData().getDetails() == null
                    || response.getData().getDetails().isEmpty()) {
                log.debug("阿里云 ISBN API 无图书数据");
                return null;
            }

            // 获取第一个结果
            BookDetail detail = response.getData().getDetails().get(0);
            if (StringUtils.isBlank(detail.getTitle())) {
                return null;
            }

            // 使用 MapStruct 转换器映射到 VO
            return bookConverter.toIsbnBookVO(detail);
        } catch (Exception e) {
            log.warn("调用阿里云 ISBN API 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Google Books API 查询图书信息
     */
    private IsbnBookVO queryFromGoogle(String isbn) {
        try {
            String query = "isbn:" + isbn;
            GoogleBooksResponse response = googleBooksClient.searchByIsbn(query, googleApiKey);

            // 检查是否有结果
            if (response == null || response.getTotalItems() == 0 || response.getItems() == null) {
                return null;
            }

            // 获取第一个结果
            VolumeInfo volumeInfo = response.getItems().get(0).getVolumeInfo();
            if (volumeInfo == null) {
                return null;
            }

            // 使用 MapStruct 转换器映射到 VO
            return bookConverter.toIsbnBookVO(volumeInfo);
        } catch (Exception e) {
            log.warn("调用 Google Books API 失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookAddVO add(BookAddDTO dto) {
        // 1. 将 DTO 转换为 Book 实体类
        Book book = bookConverter.toBook(dto);
        book.setUserId(StpUtil.getLoginIdAsLong());
        book.setIsDeleted(false);

        // 2. 保存书籍基本信息 (MyBatis-Plus 会回填 id 到 book 对象中)
        this.save(book);

        // 3. 判断书籍来源：如果是借入的 (isBorrowed == true)，保存借阅记录
        if (Boolean.TRUE.equals(dto.getIsBorrowed())) {
            BookBorrow borrow = new BookBorrow();
            borrow.setBookId(book.getId());
            borrow.setUserId(book.getUserId());
            borrow.setBorrowType(1); // 1-借入
            borrow.setBorrowerName(dto.getOwner());
            borrow.setBorrowTime(dto.getBorrowTime());
            borrow.setBookName(book.getTitle());
            borrow.setCoverUrl(book.getCoverUrl());
            borrow.setIsDeleted(false);
            bookBorrowMapper.insert(borrow);
        }

        // 4. 处理书架关联
        Long shelfId = dto.getShelfId();
        if (shelfId != null) {
            Shelf shelf = shelfMapper.selectById(shelfId);
            // 校验是否存在及权限
            if (shelf == null) {
                throw new ServiceException("指定的书架不存在");
            }
            if (!shelf.getUserId().equals(book.getUserId())) {
                throw new ServiceException("无权限添加到该书架");
            }

            // 插入关联
            BookShelf relation = new BookShelf();
            relation.setBookId(book.getId());
            relation.setShelfId(shelfId);
            relation.setIsDeleted(false);
            bookShelfMapper.insert(relation);
        }

        // 5. 封装 VO 返回给前端
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
            throw new ServiceException("无权限修改他人书籍");
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
        // 1. 校验书是否存在
        Book book = this.getById(id);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        // 2. 校验是否是本人书籍（基于登录用户）
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!book.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权限删除他人书籍");
        }

        // 3. 校验是否存在未归还的借出记录 (borrow_type=2 且 return_time is null)
        Long count = bookBorrowMapper.selectCount(
            Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, id)
                .eq(BookBorrow::getBorrowType, 2)
                .isNull(BookBorrow::getReturnTime));

        if (count > 0) {
            throw new ServiceException("书籍已借出且未归还，无法删除");
        }

        // 4. 删除关联数据
        // 4.1 删除借阅历史
        bookBorrowMapper.delete(
            Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getBookId, id));

        // 4.2 删除书架关联
        bookShelfMapper.delete(
            Wrappers.<BookShelf>lambdaQuery()
                .eq(BookShelf::getBookId, id));

        // 5. 删除书籍
        this.removeById(id);
    }

    @Override
    public void onShelf(BookShelfDTO dto) {
        // 1. 校验书是否存在
        Book book = this.getById(dto.getBookId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }
        // 2. 校验书架是否存在
        Shelf shelf = shelfMapper.selectById(dto.getShelfId());
        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }
        // 3.校验书架是否属于当前用户
        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权限将书籍放入他人书架");
        }
        // 4. 校验是否已存在关联记录
        Long count = bookShelfMapper.selectCount(new LambdaQueryWrapper<BookShelf>()
            .eq(BookShelf::getShelfId, dto.getShelfId())
            .eq(BookShelf::getBookId, dto.getBookId()));
        if (count > 0) {
            throw new ServiceException("书籍已存在于该书架");
        }
        // 5. 上架
        BookShelf bookShelf = new BookShelf();
        bookShelf.setBookId(book.getId());
        bookShelf.setShelfId(shelf.getId());
        bookShelf.setIsDeleted(false);
        bookShelfMapper.insert(bookShelf);
    }

    @Override
    public void offShelf(BookShelfDTO dto) {
        // 1. 校验书是否存在
        Book book = this.getById(dto.getBookId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }
        // 2. 校验书架是否存在
        Shelf shelf = shelfMapper.selectById(dto.getShelfId());
        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }
        // 3.校验书架是否属于当前用户
        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权限操作他人书架");
        }

        // 4. 下架
        int rows = bookShelfMapper.delete(new LambdaQueryWrapper<BookShelf>()
            .eq(BookShelf::getShelfId, dto.getShelfId())
            .eq(BookShelf::getBookId, dto.getBookId()));
        if (rows == 0) {
            throw new ServiceException("书架中未找到该书籍");
        }
    }
}
