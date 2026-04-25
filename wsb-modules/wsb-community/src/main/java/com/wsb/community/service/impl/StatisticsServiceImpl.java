package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.RemoteCollectService;
import com.wsb.book.api.dto.BookCollectCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.CollectCategoryStatsDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.BorrowStatsVO;
import com.wsb.community.api.vo.CollectStatsVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;
import com.wsb.community.convert.StatisticsConverter;
import com.wsb.community.service.StatisticsService;
import com.wsb.community.service.support.CommunityCacheService;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final RemoteBookService remoteBookService;
    private final RemoteCollectService remoteCollectService;
    private final RemoteUserService remoteUserService;
    private final StatisticsConverter statisticsConverter;
    private final CommunityCacheService communityCacheService;

    @Override
    public Page<BookRankVO> getBookRank(Integer page, Integer pageSize) {
        List<BookRankVO> cachedRanks = communityCacheService.getBookRanks();
        if (cachedRanks == null) {
            cachedRanks = buildBookRanks();
            communityCacheService.cacheBookRanks(cachedRanks);
        }
        return paginate(cachedRanks, page, pageSize);
    }

    @Override
    public Page<UserRankVO> getUserRank(Integer page, Integer pageSize) {
        List<UserRankVO> cachedRanks = communityCacheService.getUserRanks();
        if (cachedRanks == null) {
            cachedRanks = buildUserRanks();
            communityCacheService.cacheUserRanks(cachedRanks);
        }
        return paginate(cachedRanks, page, pageSize);
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

        Result<List<BorrowCategoryStatsDTO>> statsResult = remoteBookService.getBorrowStatsByCategory(bookIds);
        if (statsResult == null || statsResult.getData() == null) {
            return createEmptyBorrowStats();
        }

        List<BorrowStatsVO.CategoryStatsVO> categoryList = statisticsConverter.toBorrowCategoryStatsVOList(statsResult.getData());

        int totalSum = categoryList.stream().mapToInt(c -> c.getTotal() != null ? c.getTotal() : 0).sum();
        int readingSum = categoryList.stream().mapToInt(c -> c.getReading() != null ? c.getReading() : 0).sum();
        int readSum = categoryList.stream().mapToInt(c -> c.getRead() != null ? c.getRead() : 0).sum();

        BorrowStatsVO vo = new BorrowStatsVO();
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

        Result<List<CollectCategoryStatsDTO>> statsResult = remoteCollectService.getCollectStatsByCategory(bookIds);
        if (statsResult == null || statsResult.getData() == null) {
            return createEmptyCollectStats();
        }

        List<CollectStatsVO.CategoryCollectVO> categoryList = statisticsConverter.toCollectCategoryCollectVOList(statsResult.getData());

        int totalSum = categoryList.stream().mapToInt(c -> c.getTotal() != null ? c.getTotal() : 0).sum();
        int collectSum = categoryList.stream().mapToInt(c -> c.getCollect() != null ? c.getCollect() : 0).sum();

        CollectStatsVO vo = new CollectStatsVO();
        vo.setTotal(totalSum);
        vo.setCollect(collectSum);
        vo.setClassifyList(categoryList);
        return vo;
    }

    @Override
    public PersonalStatsVO getPersonalStats() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        PersonalStatsVO cachedStats = communityCacheService.getPersonalStats(currentUserId);
        if (cachedStats != null) {
            return cachedStats;
        }

        PersonalStatsVO stats = buildPersonalStats(currentUserId);
        communityCacheService.cachePersonalStats(currentUserId, stats);
        return stats;
    }

    private List<BookRankVO> buildBookRanks() {
        Result<List<BookCollectCountDTO>> collectResult = remoteCollectService.countCollectByBooks(null);
        if (collectResult == null || collectResult.getData() == null || collectResult.getData().isEmpty()) {
            return List.of();
        }

        List<BookCollectCountDTO> sortedList = collectResult.getData().stream()
                .sorted((a, b) -> b.getCollectCount().compareTo(a.getCollectCount()))
                .toList();

        List<Long> bookIds = sortedList.stream()
                .map(BookCollectCountDTO::getBookId)
                .toList();
        Result<List<BookRemoteDTO>> booksResult = remoteBookService.getBooksByIds(bookIds);
        Map<Long, BookRemoteDTO> bookMap = booksResult != null && booksResult.getData() != null
                ? booksResult.getData().stream().collect(Collectors.toMap(BookRemoteDTO::getId, b -> b, (a, b) -> a))
                : Map.of();

        List<BookRankVO> voList = new java.util.ArrayList<>();
        int rank = 1;
        for (BookCollectCountDTO dto : sortedList) {
            BookRemoteDTO book = bookMap.get(dto.getBookId());
            if (book != null) {
                voList.add(statisticsConverter.toBookRankVO(dto.getBookId(), book, dto.getCollectCount(), rank++));
            }
        }
        return voList;
    }

    private List<UserRankVO> buildUserRanks() {
        Result<List<UserBookCountDTO>> countResult = remoteBookService.countBooksByUsers(null);
        if (countResult == null || countResult.getData() == null || countResult.getData().isEmpty()) {
            return List.of();
        }

        List<UserBookCountDTO> sortedList = countResult.getData().stream()
                .sorted((a, b) -> b.getBookCount().compareTo(a.getBookCount()))
                .collect(Collectors.toList());

        List<Long> userIds = sortedList.stream()
                .map(UserBookCountDTO::getUserId)
                .toList();
        Result<List<UserNicknameDTO>> usersResult = remoteUserService.getUserNicknamesByIds(userIds);
        Map<Long, UserNicknameDTO> userMap = usersResult != null && usersResult.getData() != null
                ? usersResult.getData().stream().collect(Collectors.toMap(UserNicknameDTO::getId, u -> u, (a, b) -> a))
                : Map.of();

        List<UserRankVO> voList = new java.util.ArrayList<>();
        int rank = 1;
        for (UserBookCountDTO dto : sortedList) {
            UserNicknameDTO user = userMap.get(dto.getUserId());
            if (user != null) {
                voList.add(statisticsConverter.toUserRankVO(dto.getUserId(), user, dto.getBookCount(), rank++));
            }
        }
        return voList;
    }

    private PersonalStatsVO buildPersonalStats(Long currentUserId) {
        PersonalStatsVO vo = new PersonalStatsVO();

        PersonalStatsVO.OwnedStats owned = new PersonalStatsVO.OwnedStats();
        Result<List<Long>> myBooksResult = remoteBookService.getBookIdsByOwner(currentUserId);
        List<Long> myBookIds = myBooksResult != null && myBooksResult.getData() != null
                ? myBooksResult.getData()
                : List.of();

        owned.setTotalBooks(myBookIds.size());

        Result<Integer> unreturnedResult = remoteBookService.countUnreturnedByOwner(currentUserId);
        owned.setBooksLentUnreturned(unreturnedResult != null && unreturnedResult.getData() != null ? unreturnedResult.getData() : 0);

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

        Result<List<CategoryCountDTO>> categoryResult = remoteBookService.countBooksByCategory(currentUserId);
        if (categoryResult != null && categoryResult.getData() != null) {
            owned.setBooksByCategory(statisticsConverter.toCategoryCountList(categoryResult.getData()));
        } else {
            owned.setBooksByCategory(List.of());
        }
        vo.setOwned(owned);

        PersonalStatsVO.BorrowedStats borrowed = new PersonalStatsVO.BorrowedStats();
        Result<UserBorrowStatsDTO> borrowResult = remoteBookService.getUserBorrowStats(currentUserId);
        if (borrowResult != null && borrowResult.getData() != null) {
            UserBorrowStatsDTO stats = borrowResult.getData();
            borrowed.setTotalBorrowed(stats.getTotalBorrowed() != null ? stats.getTotalBorrowed() : 0);
            borrowed.setBorrowedIn(stats.getBorrowedIn() != null ? stats.getBorrowedIn() : 0);
            borrowed.setBorrowedOut(stats.getBorrowedOut() != null ? stats.getBorrowedOut() : 0);
            borrowed.setUnreturned(stats.getUnreturned() != null ? stats.getUnreturned() : 0);
        } else {
            borrowed.setTotalBorrowed(0);
            borrowed.setBorrowedIn(0);
            borrowed.setBorrowedOut(0);
            borrowed.setUnreturned(0);
        }
        vo.setBorrowed(borrowed);

        PersonalStatsVO.CollectedStats collected = new PersonalStatsVO.CollectedStats();
        Result<Integer> collectCountResult = remoteCollectService.countUserCollected(currentUserId);
        collected.setTotalCollected(collectCountResult != null && collectCountResult.getData() != null ? collectCountResult.getData() : 0);
        vo.setCollected(collected);

        return vo;
    }

    private <T> Page<T> paginate(List<T> records, Integer page, Integer pageSize) {
        int current = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int total = records.size();
        int fromIndex = (current - 1) * size;

        Page<T> result = new Page<>(current, size, total);
        if (fromIndex >= total) {
            result.setRecords(List.of());
            return result;
        }

        int toIndex = Math.min(fromIndex + size, total);
        result.setRecords(records.subList(fromIndex, toIndex));
        return result;
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
