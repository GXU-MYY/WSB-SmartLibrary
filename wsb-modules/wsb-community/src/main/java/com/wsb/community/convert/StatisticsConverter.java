package com.wsb.community.convert;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.BorrowStatsVO;
import com.wsb.community.api.vo.CollectStatsVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;
import com.wsb.social.api.dto.CollectCategoryStatsDTO;
import com.wsb.user.api.dto.UserNicknameDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 统计转换器
 */
@Mapper(componentModel = "spring")
public interface StatisticsConverter {

    // ========== 书籍排名 ==========

    @Mapping(target = "bookId", source = "bookId")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "pic", source = "book.coverUrl")
    @Mapping(target = "collectCount", source = "collectCount")
    @Mapping(target = "ranking", source = "ranking")
    BookRankVO toBookRankVO(Long bookId, BookRemoteDTO book, Integer collectCount, Integer ranking);

    // ========== 用户排名 ==========

    @Mapping(target = "id", source = "userId")
    @Mapping(target = "userName", source = "user.nickName")
    @Mapping(target = "nickName", source = "user.nickName")
    @Mapping(target = "bookCount", source = "bookCount")
    @Mapping(target = "ranking", source = "ranking")
    UserRankVO toUserRankVO(Long userId, UserNicknameDTO user, Integer bookCount, Integer ranking);

    // ========== 借阅统计 ==========

    @Mapping(target = "category", source = "category")
    @Mapping(target = "total", source = "total")
    @Mapping(target = "reading", source = "reading")
    @Mapping(target = "read", source = "read")
    BorrowStatsVO.CategoryStatsVO toBorrowCategoryStatsVO(BorrowCategoryStatsDTO dto);

    List<BorrowStatsVO.CategoryStatsVO> toBorrowCategoryStatsVOList(List<BorrowCategoryStatsDTO> dtos);

    // ========== 收藏统计 ==========

    @Mapping(target = "category", source = "category")
    @Mapping(target = "total", source = "total")
    @Mapping(target = "collect", source = "collect")
    CollectStatsVO.CategoryCollectVO toCollectCategoryCollectVO(CollectCategoryStatsDTO dto);

    List<CollectStatsVO.CategoryCollectVO> toCollectCategoryCollectVOList(List<CollectCategoryStatsDTO> dtos);

    // ========== 个人分析 - 分类统计 ==========

    @Mapping(target = "category", source = "category")
    @Mapping(target = "count", source = "count")
    PersonalStatsVO.CategoryCount toCategoryCount(CategoryCountDTO dto);

    List<PersonalStatsVO.CategoryCount> toCategoryCountList(List<CategoryCountDTO> dtos);
}