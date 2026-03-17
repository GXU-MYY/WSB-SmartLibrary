package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.borrow.api.RemoteBorrowService;
import com.wsb.borrow.api.dto.BookBorrowCountDTO;
import com.wsb.borrow.api.dto.BorrowCategoryStatsDTO;
import com.wsb.borrow.api.dto.UserBorrowStatsDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.BorrowStatsVO;
import com.wsb.community.api.vo.CollectStatsVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;
import com.wsb.community.service.StatisticsService;
import com.wsb.social.api.RemoteCollectService;
import com.wsb.social.api.dto.BookCollectCountDTO;
import com.wsb.social.api.dto.CollectCategoryStatsDTO;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final RemoteBookService remoteBookService;
    private final RemoteBorrowService remoteBorrowService;
    private final RemoteCollectService remoteCollectService;
    private final RemoteUserService remoteUserService;

    @Override
    public Page<BookRankVO> getBookRank(Integer page, Integer pageSize) {
        // 获取所有书籍收藏统计
        Result<List<BookCollectCountDTO>> collectResult = remoteCollectService.countCollectByBooks(null);
        if (collectResult == null || collectResult.getData() == null || collectResult.getData().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        // 按收藏数降序排序
        List<BookCollectCountDTO> sortedList = collectResult.getData().stream()
                .sorted((a, b) -> b.getCollectCount().compareTo(a.getCollectCount()))
                .collect(Collectors.toList());

        // 分页
        int total = sortedList.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new Page<>(page, pageSize, total);
        }

        List<BookCollectCountDTO> pageList = sortedList.subList(fromIndex, toIndex);

        // 批量获取书籍信息
        List<Long> bookIds = pageList.stream().map(BookCollectCountDTO::getBookId).collect(Collectors.toList());
        Result<List<BookRemoteDTO>> booksResult = remoteBookService.getBooksByIds(bookIds);
        Map<Long, BookRemoteDTO> bookMap = booksResult != null && booksResult.getData() != null
                ? booksResult.getData().stream().collect(Collectors.toMap(BookRemoteDTO::getId, b -> b, (a, b) -> a))
                : Map.of();

        // 组装VO
        List<BookRankVO> voList = new ArrayList<>();
        int rank = fromIndex + 1;
        for (BookCollectCountDTO dto : pageList) {
            BookRemoteDTO book = bookMap.get(dto.getBookId());
            if (book != null) {
                BookRankVO vo = new BookRankVO();
                vo.setBookId(dto.getBookId());
                vo.setTitle(book.getTitle());
                vo.setPic(book.getCoverUrl());
                vo.setCollectCount(dto.getCollectCount());
                vo.setRanking(rank++);
                voList.add(vo);
            }
        }

        Page<BookRankVO> result = new Page<>(page, pageSize, total);
        result.setRecords(voList);
        return result;
    }

    @Override
    public Page<UserRankVO> getUserRank(Integer page, Integer pageSize) {
        // 获取所有用户拥书统计
        Result<List<UserBookCountDTO>> countResult = remoteBookService.countBooksByUsers(null);
        if (countResult == null || countResult.getData() == null || countResult.getData().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        // 按书籍数降序排序
        List<UserBookCountDTO> sortedList = countResult.getData().stream()
                .sorted((a, b) -> b.getBookCount().compareTo(a.getBookCount()))
                .collect(Collectors.toList());

        // 分页
        int total = sortedList.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new Page<>(page, pageSize, total);
        }

        List<UserBookCountDTO> pageList = sortedList.subList(fromIndex, toIndex);

        // 批量获取用户信息
        List<Long> userIds = pageList.stream().map(UserBookCountDTO::getUserId).collect(Collectors.toList());
        Result<List<UserNicknameDTO>> usersResult = remoteUserService.getUserNicknamesByIds(userIds);
        Map<Long, UserNicknameDTO> userMap = usersResult != null && usersResult.getData() != null
                ? usersResult.getData().stream().collect(Collectors.toMap(UserNicknameDTO::getId, u -> u, (a, b) -> a))
                : Map.of();

        // 组装VO
        List<UserRankVO> voList = new ArrayList<>();
        int rank = fromIndex + 1;
        for (UserBookCountDTO dto : pageList) {
            UserNicknameDTO user = userMap.get(dto.getUserId());
            if (user != null) {
                UserRankVO vo = new UserRankVO();
                vo.setId(dto.getUserId());
                vo.setUserName(user.getNickName());
                vo.setNickName(user.getNickName());
                vo.setBookCount(dto.getBookCount());
                vo.setRanking(rank++);
                voList.add(vo);
            }
        }

        Page<UserRankVO> result = new Page<>(page, pageSize, total);
        result.setRecords(voList);
        return result;
    }

    @Override
    public BorrowStatsVO getBorrowStats(String scope) {
        List<Long> bookIds = null;

        if ("mine".equals(scope)) {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            Result<List<Long>> booksResult = remoteBookService.getBookIdsByOwner(currentUserId);
            if (booksResult == null || booksResult.getData() == null) {
                return createEmptyBorrowStats();
            }
            bookIds = booksResult.getData();
        }

        // 获取借阅分类统计
        Result<List<BorrowCategoryStatsDTO>> statsResult = remoteBorrowService.getBorrowStatsByCategory(bookIds);
        if (statsResult == null || statsResult.getData() == null) {
            return createEmptyBorrowStats();
        }

        BorrowStatsVO vo = new BorrowStatsVO();
        int totalSum = 0, readingSum = 0, readSum = 0;
        List<BorrowStatsVO.CategoryStatsVO> categoryList = new ArrayList<>();

        for (BorrowCategoryStatsDTO dto : statsResult.getData()) {
            BorrowStatsVO.CategoryStatsVO categoryVO = new BorrowStatsVO.CategoryStatsVO();
            categoryVO.setCategory(dto.getCategory());
            categoryVO.setTotal(dto.getTotal());
            categoryVO.setReading(dto.getReading());
            categoryVO.setRead(dto.getRead());
            categoryList.add(categoryVO);

            totalSum += dto.getTotal() != null ? dto.getTotal() : 0;
            readingSum += dto.getReading() != null ? dto.getReading() : 0;
            readSum += dto.getRead() != null ? dto.getRead() : 0;
        }

        vo.setTotal(totalSum);
        vo.setReading(readingSum);
        vo.setRead(readSum);
        vo.setClassifyList(categoryList);
        return vo;
    }

    @Override
    public CollectStatsVO getCollectStats(String scope) {
        List<Long> bookIds = null;

        if ("mine".equals(scope)) {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            Result<List<Long>> booksResult = remoteBookService.getBookIdsByOwner(currentUserId);
            if (booksResult == null || booksResult.getData() == null) {
                return createEmptyCollectStats();
            }
            bookIds = booksResult.getData();
        }

        // 获取收藏分类统计
        Result<List<CollectCategoryStatsDTO>> statsResult = remoteCollectService.getCollectStatsByCategory(bookIds);
        if (statsResult == null || statsResult.getData() == null) {
            return createEmptyCollectStats();
        }

        CollectStatsVO vo = new CollectStatsVO();
        int totalSum = 0, collectSum = 0;
        List<CollectStatsVO.CategoryCollectVO> categoryList = new ArrayList<>();

        for (CollectCategoryStatsDTO dto : statsResult.getData()) {
            CollectStatsVO.CategoryCollectVO categoryVO = new CollectStatsVO.CategoryCollectVO();
            categoryVO.setCategory(dto.getCategory());
            categoryVO.setTotal(dto.getTotal());
            categoryVO.setCollect(dto.getCollect());
            categoryList.add(categoryVO);

            totalSum += dto.getTotal() != null ? dto.getTotal() : 0;
            collectSum += dto.getCollect() != null ? dto.getCollect() : 0;
        }

        vo.setTotal(totalSum);
        vo.setCollect(collectSum);
        vo.setClassifyList(categoryList);
        return vo;
    }

    @Override
    public PersonalStatsVO getPersonalStats() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        PersonalStatsVO vo = new PersonalStatsVO();

        // 我拥有的书籍统计
        PersonalStatsVO.OwnedStats owned = new PersonalStatsVO.OwnedStats();
        Result<List<Long>> myBooksResult = remoteBookService.getBookIdsByOwner(currentUserId);
        List<Long> myBookIds = myBooksResult != null && myBooksResult.getData() != null
                ? myBooksResult.getData() : List.of();

        owned.setTotalBooks(myBookIds.size());

        // 借出未归还数
        Result<Integer> unreturnedResult = remoteBorrowService.countUnreturnedByOwner(currentUserId);
        owned.setBooksLentUnreturned(unreturnedResult != null ? unreturnedResult.getData() : 0);

        // 被收藏数
        if (!myBookIds.isEmpty()) {
            Result<List<BookCollectCountDTO>> collectResult = remoteCollectService.countCollectByBooks(myBookIds);
            int totalCollect = 0;
            if (collectResult != null && collectResult.getData() != null) {
                totalCollect = collectResult.getData().stream()
                        .mapToInt(dto -> dto.getCollectCount() != null ? dto.getCollectCount() : 0)
                        .sum();
            }
            owned.setBooksBeingCollected(totalCollect);
        } else {
            owned.setBooksBeingCollected(0);
        }

        // 按分类统计
        Result<List<CategoryCountDTO>> categoryResult = remoteBookService.countBooksByCategory(currentUserId);
        List<PersonalStatsVO.CategoryCount> categoryList = new ArrayList<>();
        if (categoryResult != null && categoryResult.getData() != null) {
            for (CategoryCountDTO dto : categoryResult.getData()) {
                PersonalStatsVO.CategoryCount cc = new PersonalStatsVO.CategoryCount();
                cc.setCategory(dto.getCategory());
                cc.setCount(dto.getCount());
                categoryList.add(cc);
            }
        }
        owned.setBooksByCategory(categoryList);
        vo.setOwned(owned);

        // 我借阅的书籍统计
        PersonalStatsVO.BorrowedStats borrowed = new PersonalStatsVO.BorrowedStats();
        Result<UserBorrowStatsDTO> borrowResult = remoteBorrowService.getUserBorrowStats(currentUserId);
        if (borrowResult != null && borrowResult.getData() != null) {
            UserBorrowStatsDTO stats = borrowResult.getData();
            borrowed.setTotalBorrowed(stats.getTotalBorrowed() != null ? stats.getTotalBorrowed() : 0);
            borrowed.setUnreturned(stats.getUnreturned() != null ? stats.getUnreturned() : 0);
        } else {
            borrowed.setTotalBorrowed(0);
            borrowed.setUnreturned(0);
        }
        vo.setBorrowed(borrowed);

        // 我收藏的书籍统计
        PersonalStatsVO.CollectedStats collected = new PersonalStatsVO.CollectedStats();
        Result<Integer> collectCountResult = remoteCollectService.countUserCollected(currentUserId);
        collected.setTotalCollected(collectCountResult != null ? collectCountResult.getData() : 0);
        vo.setCollected(collected);

        return vo;
    }

    private BorrowStatsVO createEmptyBorrowStats() {
        BorrowStatsVO vo = new BorrowStatsVO();
        vo.setTotal(0);
        vo.setReading(0);
        vo.setRead(0);
        vo.setClassifyList(List.of());
        return vo;
    }

    private CollectStatsVO createEmptyCollectStats() {
        CollectStatsVO vo = new CollectStatsVO();
        vo.setTotal(0);
        vo.setCollect(0);
        vo.setClassifyList(List.of());
        return vo;
    }
}