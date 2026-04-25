package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.CollectAddDTO;
import com.wsb.book.api.dto.CollectDeleteDTO;
import com.wsb.book.api.vo.CollectBookVO;
import com.wsb.book.api.vo.CollectVO;
import com.wsb.book.convert.CollectConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.Collect;
import com.wsb.book.mapper.CollectMapper;
import com.wsb.book.service.BookService;
import com.wsb.book.service.CollectService;
import com.wsb.book.service.support.CommunityStatisticsCacheEvictService;
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
    private final CommunityStatisticsCacheEvictService communityStatisticsCacheEvictService;

    @Override
    public CollectVO addCollect(CollectAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        if (dto.getBookId() == null) {
            throw new ServiceException("请选择要收藏的图书");
        }

        Book book = bookService.getById(dto.getBookId());
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
            throw new ServiceException("图书不存在");
        }

        Long targetId = dto.getBookId();

        Collect collect = this.getOne(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, currentUserId)
                .eq(Collect::getTargetId, targetId)
                .eq(Collect::getCollectType, 1)
                .last("limit 1"));
        if (collect != null && !Boolean.TRUE.equals(collect.getIsDeleted())) {
            throw new ServiceException("已收藏该图书");
        }

        if (collect != null) {
            collect.setIsDeleted(false);
            this.updateById(collect);
            evictStatisticsCaches(currentUserId, book.getUserId());
            return collectConverter.toCollectVO(collect);
        }

        collect = new Collect();
        collect.setUserId(currentUserId);
        collect.setTargetId(targetId);
        collect.setCollectType(1);
        collect.setIsDeleted(false);
        this.save(collect);
        evictStatisticsCaches(currentUserId, book.getUserId());
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
        Book book = bookService.getById(collect.getTargetId());
        evictStatisticsCaches(currentUserId, book != null ? book.getUserId() : null);
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

    private void evictStatisticsCaches(Long collectorUserId, Long ownerUserId) {
        communityStatisticsCacheEvictService.evictPersonalStats(collectorUserId);
        if (!Objects.equals(collectorUserId, ownerUserId)) {
            communityStatisticsCacheEvictService.evictPersonalStats(ownerUserId);
        }
        communityStatisticsCacheEvictService.evictBookRank();
    }

}
