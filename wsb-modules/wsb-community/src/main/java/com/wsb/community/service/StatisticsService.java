package com.wsb.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.BorrowStatsVO;
import com.wsb.community.api.vo.CollectStatsVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取书籍收藏排名
     */
    Page<BookRankVO> getBookRank(Integer page, Integer pageSize);

    /**
     * 获取用户拥书排名
     */
    Page<UserRankVO> getUserRank(Integer page, Integer pageSize);

    /**
     * 获取借阅统计
     *
     * @param scope all-全站, mine-我的
     */
    BorrowStatsVO getBorrowStats(String scope);

    /**
     * 获取收藏统计
     *
     * @param scope all-全站, mine-我的
     */
    CollectStatsVO getCollectStats(String scope);

    /**
     * 获取个人分析统计
     */
    PersonalStatsVO getPersonalStats();
}