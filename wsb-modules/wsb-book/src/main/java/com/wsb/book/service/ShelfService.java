package com.wsb.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.book.api.dto.ShelfAddDTO;
import com.wsb.book.api.dto.ShelfUpdateDTO;
import com.wsb.book.api.vo.ShelfVO;
import com.wsb.book.domain.Shelf;

import java.util.List;

/**
 * 书架服务接口
 */
public interface ShelfService extends IService<Shelf> {
    ShelfVO add(ShelfAddDTO dto);
    ShelfVO update(ShelfUpdateDTO dto);
    List<ShelfVO> detail(Long userId, Long id);
    ShelfVO getByBookId(Long bookId);
    void delete(Long shelfId);
    Page<ShelfVO> list(Integer page, Integer pageSize, String shelfType, String shelfName, Long userId);
}
