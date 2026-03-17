package com.wsb.social.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.social.api.dto.CommentAddDTO;
import com.wsb.social.api.dto.CommentDeleteDTO;
import com.wsb.social.api.vo.BookCommentListVO;
import com.wsb.social.api.vo.CommentVO;
import com.wsb.social.api.vo.TopRatedBookVO;
import com.wsb.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@Tag(name = "评论管理")
@RestController
@RequestMapping("/v1/social/book/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "书籍点评")
    @PostMapping
    public Result<CommentVO> addComment(@Valid @RequestBody CommentAddDTO dto) {
        return Result.success(commentService.addComment(dto));
    }

    @Operation(summary = "书籍评论列表", description = "带book_id参数返回书本评论，不带book_id参数返回个人所有评论")
    @GetMapping
    public Result<?> getComments(
            @Parameter(description = "书籍ID，不带则返回个人评论")
            @RequestParam(value = "book_id", required = false) Long bookId) {
        if (bookId != null) {
            return Result.success(commentService.getBookComments(bookId));
        } else {
            return Result.success(commentService.getMyComments());
        }
    }

    @Operation(summary = "评论删除")
    @DeleteMapping
    public Result<Void> deleteComment(@RequestBody CommentDeleteDTO dto) {
        commentService.deleteComment(dto);
        return Result.success();
    }
}