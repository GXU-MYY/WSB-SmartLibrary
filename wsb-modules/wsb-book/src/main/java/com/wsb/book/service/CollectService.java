package com.wsb.book.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.book.api.dto.CollectAddDTO;
import com.wsb.book.api.dto.CollectDeleteDTO;
import com.wsb.book.api.vo.CollectBookVO;
import com.wsb.book.api.vo.CollectShelfVO;
import com.wsb.book.api.vo.CollectVO;
import com.wsb.book.domain.Collect;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface CollectService extends IService<Collect> {

    /**
     * 添加收藏
     */
    CollectVO addCollect(CollectAddDTO dto);

    /**
     * 取消收藏
     */
    void deleteCollect(CollectDeleteDTO dto);

    /**
     * 获取我的图书收藏
     */
    List<CollectBookVO> getMyBookCollects();

    /**
     * 获取我的书架收藏
     */
    List<CollectShelfVO> getMyShelfCollects();
}
