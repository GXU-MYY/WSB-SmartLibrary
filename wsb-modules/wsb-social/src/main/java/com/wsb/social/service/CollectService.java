package com.wsb.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.social.api.dto.CollectAddDTO;
import com.wsb.social.api.dto.CollectDeleteDTO;
import com.wsb.social.api.vo.CollectBookVO;
import com.wsb.social.api.vo.CollectShelfVO;
import com.wsb.social.api.vo.CollectVO;
import com.wsb.social.domain.Collect;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface CollectService extends IService<Collect> {

    /**
     * 添加收藏（书籍或书架）
     */
    CollectVO addCollect(CollectAddDTO dto);

    /**
     * 取消收藏
     */
    void deleteCollect(CollectDeleteDTO dto);

    /**
     * 获取我的书籍收藏列表
     */
    List<CollectBookVO> getMyBookCollects();

    /**
     * 获取我的书架收藏列表
     */
    List<CollectShelfVO> getMyShelfCollects();
}