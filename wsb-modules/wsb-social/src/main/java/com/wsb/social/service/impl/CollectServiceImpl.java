package com.wsb.social.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.social.api.dto.CollectAddDTO;
import com.wsb.social.api.dto.CollectDeleteDTO;
import com.wsb.social.api.vo.CollectBookVO;
import com.wsb.social.api.vo.CollectShelfVO;
import com.wsb.social.api.vo.CollectVO;
import com.wsb.social.convert.CollectConverter;
import com.wsb.social.domain.Collect;
import com.wsb.social.mapper.CollectMapper;
import com.wsb.social.service.CollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类
 */
@Service
@RequiredArgsConstructor
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {

    private final CollectConverter collectConverter;
    private final RemoteBookService remoteBookService;

    @Override
    public CollectVO addCollect(CollectAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 校验参数：bookId和bookshelfId只能填一个
        if ((dto.getBookId() == null) == (dto.getBookshelfId() == null)) {
            throw new ServiceException("请选择收藏书籍或收藏书架");
        }

        Collect collect = new Collect();
        collect.setUserId(currentUserId);
        collect.setIsDeleted(false);

        if (dto.getBookId() != null) {
            // 收藏书籍
            Result<BookRemoteDTO> bookResult = remoteBookService.getBookById(dto.getBookId());
            if (bookResult == null || bookResult.getData() == null) {
                throw new ServiceException("书籍不存在");
            }

            // 检查是否已收藏
            boolean exists = this.exists(Wrappers.<Collect>lambdaQuery()
                    .eq(Collect::getUserId, currentUserId)
                    .eq(Collect::getTargetId, dto.getBookId())
                    .eq(Collect::getCollectType, 1)
                    .eq(Collect::getIsDeleted, false));
            if (exists) {
                throw new ServiceException("已收藏该书籍");
            }

            collect.setTargetId(dto.getBookId());
            collect.setCollectType(1);
        } else {
            // 收藏书架
            Result<ShelfRemoteDTO> shelfResult = remoteBookService.getShelfById(dto.getBookshelfId());
            if (shelfResult == null || shelfResult.getData() == null) {
                throw new ServiceException("书架不存在");
            }

            // 检查是否已收藏
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
        if (collect == null || collect.getIsDeleted()) {
            throw new ServiceException("收藏记录不存在");
        }

        // 校验是否是本人的收藏
        if (!collect.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权限删除他人收藏");
        }

        // 软删除
        collect.setIsDeleted(true);
        this.updateById(collect);
    }

    @Override
    public List<CollectBookVO> getMyBookCollects() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询书籍收藏
        List<Collect> collects = this.list(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, currentUserId)
                .eq(Collect::getCollectType, 1)
                .eq(Collect::getIsDeleted, false)
                .orderByDesc(Collect::getCreateTime));

        if (collects.isEmpty()) {
            return List.of();
        }

        // 批量获取书籍信息
        List<Long> bookIds = collects.stream()
                .map(Collect::getTargetId)
                .collect(Collectors.toList());
        Result<List<BookRemoteDTO>> booksResult = remoteBookService.getBooksByIds(bookIds);
        if (booksResult == null || booksResult.getData() == null) {
            return List.of();
        }

        Map<Long, BookRemoteDTO> bookMap = booksResult.getData().stream()
                .collect(Collectors.toMap(BookRemoteDTO::getId, b -> b, (a, b) -> a));

        // 转换VO
        return collects.stream()
                .map(c -> {
                    BookRemoteDTO book = bookMap.get(c.getTargetId());
                    if (book != null) {
                        return collectConverter.toCollectBookVO(book, c.getId(), c.getCreateTime());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectShelfVO> getMyShelfCollects() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询书架收藏
        List<Collect> collects = this.list(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, currentUserId)
                .eq(Collect::getCollectType, 2)
                .eq(Collect::getIsDeleted, false)
                .orderByDesc(Collect::getCreateTime));

        if (collects.isEmpty()) {
            return List.of();
        }

        // 批量获取书架信息
        List<Long> shelfIds = collects.stream()
                .map(Collect::getTargetId)
                .collect(Collectors.toList());
        Result<List<ShelfRemoteDTO>> shelvesResult = remoteBookService.getShelfByIds(shelfIds);
        if (shelvesResult == null || shelvesResult.getData() == null) {
            return List.of();
        }

        Map<Long, ShelfRemoteDTO> shelfMap = shelvesResult.getData().stream()
                .collect(Collectors.toMap(ShelfRemoteDTO::getId, s -> s, (a, b) -> a));

        // 转换VO
        return collects.stream()
                .map(c -> {
                    ShelfRemoteDTO shelf = shelfMap.get(c.getTargetId());
                    if (shelf != null) {
                        return collectConverter.toCollectShelfVO(shelf, c.getId(), c.getCreateTime());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}