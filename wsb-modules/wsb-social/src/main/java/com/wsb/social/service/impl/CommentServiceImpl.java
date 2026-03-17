package com.wsb.social.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.social.api.dto.CommentAddDTO;
import com.wsb.social.api.dto.CommentDeleteDTO;
import com.wsb.social.api.vo.BookCommentListVO;
import com.wsb.social.api.vo.CommentVO;
import com.wsb.social.api.vo.TopRatedBookVO;
import com.wsb.social.convert.CommentConverter;
import com.wsb.social.domain.Comment;
import com.wsb.social.mapper.CommentMapper;
import com.wsb.social.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentConverter commentConverter;
    private final RemoteBookService remoteBookService;

    @Override
    public CommentVO addComment(CommentAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 验证书籍存在
        Result<BookRemoteDTO> bookResult = remoteBookService.getBookById(dto.getBookId());
        if (bookResult == null || bookResult.getData() == null) {
            throw new ServiceException("书籍不存在");
        }

        // 使用 Converter 转换并设置额外字段
        Comment comment = commentConverter.toComment(dto);
        comment.setUserId(currentUserId);
        this.save(comment);

        return commentConverter.toCommentVO(comment);
    }

    @Override
    public BookCommentListVO getBookComments(Long bookId) {
        BookCommentListVO result = new BookCommentListVO();

        // 查询该书籍的所有评论
        List<Comment> comments = this.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBookId, bookId)
                .eq(Comment::getIsDeleted, false)
                .orderByDesc(Comment::getCreateTime));

        List<CommentVO> commentVOList = commentConverter.toCommentVOList(comments);
        result.setComments(commentVOList);

        // 计算平均评分
        if (!comments.isEmpty()) {
            double avgStar = comments.stream()
                    .filter(c -> c.getStarRating() != null)
                    .mapToInt(Comment::getStarRating)
                    .average()
                    .orElse(0);
            result.setStarMean((int) Math.round(avgStar));
        } else {
            result.setStarMean(0);
        }

        return result;
    }

    @Override
    public List<CommentVO> getMyComments() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        List<Comment> comments = this.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getUserId, currentUserId)
                .eq(Comment::getIsDeleted, false)
                .orderByDesc(Comment::getCreateTime));

        return commentConverter.toCommentVOList(comments);
    }

    @Override
    public void deleteComment(CommentDeleteDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        Comment comment = this.getById(dto.getCommentId());
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        // 校验是否是本人的评论
        if (!comment.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权限删除他人评论");
        }

        // 软删除
        comment.setIsDeleted(true);
        this.updateById(comment);
    }

    @Override
    public List<TopRatedBookVO> getTopRatedBooks(Integer top) {
        if (top == null || top <= 0) {
            top = 5;
        }

        // 查询评分最高的书籍ID（按平均评分排序）
        List<Comment> comments = this.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getIsDeleted, false)
                .isNotNull(Comment::getStarRating));

        if (comments.isEmpty()) {
            return List.of();
        }

        // 按书籍ID分组，计算平均评分
        Map<Long, Double> bookAvgStars = comments.stream()
                .collect(Collectors.groupingBy(
                        Comment::getBookId,
                        Collectors.averagingInt(Comment::getStarRating)
                ));

        // 按平均评分降序排序，取前top条
        List<Long> topBookIds = bookAvgStars.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(top)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topBookIds.isEmpty()) {
            return List.of();
        }

        // 批量获取书籍信息
        Result<List<BookRemoteDTO>> booksResult = remoteBookService.getBooksByIds(topBookIds);
        if (booksResult == null || booksResult.getData() == null) {
            return List.of();
        }

        Map<Long, BookRemoteDTO> bookMap = booksResult.getData().stream()
                .collect(Collectors.toMap(BookRemoteDTO::getId, b -> b, (a, b) -> a));

        // 使用 Converter 转换（保持排序）
        return topBookIds.stream()
                .map(bookId -> {
                    BookRemoteDTO book = bookMap.get(bookId);
                    if (book != null) {
                        Integer stars = (int) Math.round(bookAvgStars.get(bookId));
                        return commentConverter.toTopRatedBookVO(book, stars);
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}