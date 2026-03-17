package com.wsb.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.social.api.dto.CommentAddDTO;
import com.wsb.social.api.dto.CommentDeleteDTO;
import com.wsb.social.api.vo.BookCommentListVO;
import com.wsb.social.api.vo.CommentVO;
import com.wsb.social.api.vo.TopRatedBookVO;
import com.wsb.social.domain.Comment;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     */
    CommentVO addComment(CommentAddDTO dto);

    /**
     * 获取书籍评论列表
     *
     * @param bookId 书籍ID，为空则返回当前用户的评论
     */
    BookCommentListVO getBookComments(Long bookId);

    /**
     * 获取个人评论列表
     */
    List<CommentVO> getMyComments();

    /**
     * 删除评论
     */
    void deleteComment(CommentDeleteDTO dto);

    /**
     * 获取评分最高的书籍列表
     *
     * @param top 返回前N条，默认5条
     */
    List<TopRatedBookVO> getTopRatedBooks(Integer top);
}