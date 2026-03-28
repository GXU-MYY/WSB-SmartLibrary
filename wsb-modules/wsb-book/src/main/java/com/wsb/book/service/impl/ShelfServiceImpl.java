package com.wsb.book.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.dto.ShelfAddDTO;
import com.wsb.book.api.dto.ShelfUpdateDTO;
import com.wsb.book.api.vo.ShelfVO;
import com.wsb.book.convert.ShelfConvert;
import com.wsb.book.domain.BookShelf;
import com.wsb.book.domain.Shelf;
import com.wsb.book.mapper.BookShelfMapper;
import com.wsb.book.mapper.ShelfMapper;
import com.wsb.book.service.ShelfService;
import com.wsb.common.core.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 书架服务实现类
 */
@Service
@Slf4j
@AllArgsConstructor
public class ShelfServiceImpl extends ServiceImpl<ShelfMapper, Shelf> implements ShelfService {
    private final ShelfConvert shelfConvert;
    private final BookShelfMapper bookShelfMapper;

    @Override
    public ShelfVO add(ShelfAddDTO dto) {
        Shelf shelf = shelfConvert.toShelf(dto);
        shelf.setUserId(StpUtil.getLoginIdAsLong());
        this.save(shelf);
        return shelfConvert.toShelfVO(shelf);
    }

    @Override
    public ShelfVO update(ShelfUpdateDTO dto) {
        Shelf shelf = this.getById(dto.getId());

        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }

        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权限");
        }

        shelfConvert.updateShelfFromDto(dto, shelf);
        this.updateById(shelf);
        return shelfConvert.toShelfVO(shelf);
    }

    /*
     * 查询书架详情，优先级： userId > id
     * 如果 userId 不为空，返回公开的书架/书单
     * 如果 id 不为空，根据 id 查询书架详情
     * 都为空，返回个人所有书架/书单
     * @param userId 用户ID
     * @param id 书架ID
     * @return ShelfVO
     */
    @Override
    public List<ShelfVO> detail(Long userId, Long id) {
        List<Shelf> list = null;
        if (userId != null) {
            list = this.lambdaQuery()
                .eq(Shelf::getUserId, userId)
                .eq(Shelf::getIsPublic, true) // 他人只能看公开的
                .orderByDesc(Shelf::getCreateTime)
                .list();
        } else if (id != null) {
            Shelf shelf = this.getById(id);
            if (shelf != null) {
                if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong()) && !Boolean.TRUE.equals(shelf.getIsPublic())) {
                    throw new ServiceException("无权限");
                }
                list = List.of(shelf);
            }
        } else {
            list = this.lambdaQuery()
                .eq(Shelf::getUserId, StpUtil.getLoginIdAsLong())
                .orderByDesc(Shelf::getCreateTime)
                .list();
        }
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return shelfConvert.toShelfVOList(list);
    }

    @Override
    public ShelfVO getByBookId(Long bookId) {
        if (bookId == null) {
            return null;
        }

        List<BookShelf> relations = bookShelfMapper.selectList(new LambdaQueryWrapper<BookShelf>()
                .eq(BookShelf::getBookId, bookId)
                .orderByAsc(BookShelf::getShelfId));
        if (relations.isEmpty()) {
            return null;
        }
        if (relations.size() > 1) {
            log.warn("图书存在多个书架关联，将返回第一条关系: bookId={}, count={}", bookId, relations.size());
        }

        Shelf shelf = this.lambdaQuery()
                .eq(Shelf::getId, relations.get(0).getShelfId())
                .eq(Shelf::getUserId, StpUtil.getLoginIdAsLong())
                .one();
        if (shelf == null) {
            return null;
        }

        return shelfConvert.toShelfVO(shelf);
    }

    @Override
    @Transactional
    public void delete(Long shelfId) {
        Shelf shelf = this.getById(shelfId);
        if (shelf == null) {
            throw new ServiceException("书架不存在");
        }
        if (!shelf.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("无权限");
        }

        // 删除书架下的书籍关联
        bookShelfMapper.delete(new LambdaQueryWrapper<BookShelf>()
            .eq(BookShelf::getShelfId, shelfId));

        // 物理删除书架
        this.removeById(shelfId);
    }

    @Override
    public Page<ShelfVO> list(Integer page, Integer pageSize, String shelfType, String shelfName, Long userId) {
        Page<Shelf> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Shelf> wrapper = new LambdaQueryWrapper<>();

        // 1. 处理用户权限
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (userId != null) {
            wrapper.eq(Shelf::getUserId, userId);
            // 如果查询的是他人书架，则只能看公开的
            if (!userId.equals(currentUserId)) {
                wrapper.eq(Shelf::getIsPublic, true);
            }
        } else {
            // 默认查询当前登录用户的书架
            wrapper.eq(Shelf::getUserId, currentUserId);
        }

        // 2. 处理查询条件
        if (shelfType != null && !shelfType.isEmpty()) {
            wrapper.eq(Shelf::getShelfType, Integer.valueOf(shelfType));
        }
        if (shelfName != null && !shelfName.isEmpty()) {
            wrapper.like(Shelf::getShelfName, shelfName);
        }

        // 3. 排序
        wrapper.orderByDesc(Shelf::getCreateTime);

        // 4. 执行查询
        this.page(pageParam, wrapper);

        // 5. 转换结果
        Page<ShelfVO> resultPage = new Page<>();
        resultPage.setCurrent(pageParam.getCurrent());
        resultPage.setSize(pageParam.getSize());
        resultPage.setTotal(pageParam.getTotal());
        resultPage.setPages(pageParam.getPages());
        resultPage.setRecords(shelfConvert.toShelfVOList(pageParam.getRecords()));

        return resultPage;
    }
}
