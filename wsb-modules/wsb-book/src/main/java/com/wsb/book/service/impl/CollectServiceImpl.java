package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.CollectAddDTO;
import com.wsb.book.api.dto.CollectDeleteDTO;
import com.wsb.book.api.vo.CollectBookVO;
import com.wsb.book.api.vo.CollectShelfVO;
import com.wsb.book.api.vo.CollectVO;
import com.wsb.book.convert.CollectConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.Collect;
import com.wsb.book.domain.Shelf;
import com.wsb.book.mapper.CollectMapper;
import com.wsb.book.service.BookService;
import com.wsb.book.service.CollectService;
import com.wsb.book.service.ShelfService;
import com.wsb.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类
 */
@Service
@RequiredArgsConstructor
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {

    private final CollectConverter collectConverter;
    private final BookService bookService;
    private final ShelfService shelfService;

    @Override
    public CollectVO addCollect(CollectAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        if ((dto.getBookId() == null) == (dto.getBookshelfId() == null)) {
            throw new ServiceException("请选择要收藏的图书或书架");
        }

        Collect collect = new Collect();
        collect.setUserId(currentUserId);
        collect.setIsDeleted(false);

        if (dto.getBookId() != null) {
            Book book = bookService.getById(dto.getBookId());
            if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
                throw new ServiceException("图书不存在");
            }

            boolean exists = this.exists(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getUserId, currentUserId)
                    .eq(Collect::getTargetId, dto.getBookId())
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
            if (exists) {
                throw new ServiceException("已收藏该图书");
            }

            collect.setTargetId(dto.getBookId());
            collect.setCollectType(1);
        } else {
            Shelf shelf = shelfService.getById(dto.getBookshelfId());
            if (shelf == null || Boolean.TRUE.equals(shelf.getIsDeleted())) {
                throw new ServiceException("书架不存在");
            }

            boolean exists = this.exists(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getUserId, currentUserId)
                    .eq(Collect::getTargetId, dto.getBookshelfId())
                    .eq(Collect::getCollectType, 2)
                    .eq(Collect::getIsDeleted, false));
            if (exists) {
                throw new ServiceException("已收藏该书架");
            }

            collect.setTargetId(dto.getBookshelfId());
            collect.setCollectType(2);
        }

        this.save(collect);
        return collectConverter.toCollectVO(collect);
    }

    @Override
    public void deleteCollect(CollectDeleteDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        Collect collect = this.getById(dto.getCollectId());
        if (collect == null || Boolean.TRUE.equals(collect.getIsDeleted())) {
            throw new ServiceException("收藏记录不存在");
        }

        if (!collect.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权限删除他人收藏");
        }

        collect.setIsDeleted(true);
        this.updateById(collect);
    }

    @Override
    public List<CollectBookVO> getMyBookCollects() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<Collect> collects = this.list(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, currentUserId)
                .eq(Collect::getCollectType, 1)
                .eq(Collect::getIsDeleted, false)
                .orderByDesc(Collect::getCreateTime));

        if (collects.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = collects.stream()
                .map(Collect::getTargetId)
                .toList();
        List<Book> books = bookService.listByIds(bookIds);
        var bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity(), (a, b) -> a));

        return collects.stream()
                .map(collect -> {
                    Book book = bookMap.get(collect.getTargetId());
                    if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
                        return null;
                    }
                    return collectConverter.toCollectBookVO(book, collect.getId(), collect.getCreateTime());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<CollectShelfVO> getMyShelfCollects() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<Collect> collects = this.list(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, currentUserId)
                .eq(Collect::getCollectType, 2)
                .eq(Collect::getIsDeleted, false)
                .orderByDesc(Collect::getCreateTime));

        if (collects.isEmpty()) {
            return List.of();
        }

        List<Long> shelfIds = collects.stream()
                .map(Collect::getTargetId)
                .toList();
        List<Shelf> shelves = shelfService.listByIds(shelfIds);
        var shelfMap = shelves.stream().collect(Collectors.toMap(Shelf::getId, Function.identity(), (a, b) -> a));

        return collects.stream()
                .map(collect -> {
                    Shelf shelf = shelfMap.get(collect.getTargetId());
                    if (shelf == null || Boolean.TRUE.equals(shelf.getIsDeleted())) {
                        return null;
                    }
                    return collectConverter.toCollectShelfVO(shelf, collect.getId(), collect.getCreateTime());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
