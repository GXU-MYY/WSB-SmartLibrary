package com.wsb.social.convert;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.social.api.dto.CommentAddDTO;
import com.wsb.social.api.vo.CommentVO;
import com.wsb.social.api.vo.TopRatedBookVO;
import com.wsb.social.domain.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 评论转换器
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentConverter {

    // ========== DTO -> Entity ==========

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userNickname", ignore = true)
    @Mapping(target = "userAvatar", ignore = true)
    @Mapping(target = "content", source = "comment")
    @Mapping(target = "likeCount", constant = "0")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Comment toComment(CommentAddDTO dto);

    // ========== Entity -> VO ==========

    @Mapping(target = "comment", source = "content")
    @Mapping(target = "comTime", source = "createTime")
    @Mapping(target = "stars", source = "starRating")
    CommentVO toCommentVO(Comment comment);

    List<CommentVO> toCommentVOList(List<Comment> comments);

    // ========== BookRemoteDTO -> TopRatedBookVO ==========

    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "pic", source = "book.coverUrl")
    @Mapping(target = "stars", source = "stars")
    TopRatedBookVO toTopRatedBookVO(BookRemoteDTO book, Integer stars);
}