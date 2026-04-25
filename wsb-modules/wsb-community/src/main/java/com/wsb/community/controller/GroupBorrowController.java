package com.wsb.community.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.community.api.dto.GroupBorrowRequestAddDTO;
import com.wsb.community.api.vo.GroupBorrowRequestVO;
import com.wsb.community.api.vo.GroupPublicBookVO;
import com.wsb.community.api.vo.GroupPublicShelfVO;
import com.wsb.community.service.GroupBorrowRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 群组借阅控制器
 */
@Tag(name = "群组借阅")
@RestController
@RequestMapping("/v1/group/borrow")
@RequiredArgsConstructor
public class GroupBorrowController {

    private final GroupBorrowRequestService groupBorrowRequestService;

    @Operation(summary = "群组公开书架列表")
    @GetMapping("/public/shelves")
    public Result<List<GroupPublicShelfVO>> getPublicShelves(
            @Parameter(description = "群组ID", required = true)
            @RequestParam("group_id") Long groupId) {
        return Result.success(groupBorrowRequestService.getPublicShelves(groupId));
    }

    @Operation(summary = "群组公开图书列表")
    @GetMapping("/public/books")
    public Result<List<GroupPublicBookVO>> getPublicBooks(
            @Parameter(description = "群组ID", required = true)
            @RequestParam("group_id") Long groupId) {
        return Result.success(groupBorrowRequestService.getPublicBooks(groupId));
    }

    @Operation(summary = "发起群组借阅申请")
    @PostMapping("/request")
    public Result<GroupBorrowRequestVO> createRequest(@Valid @RequestBody GroupBorrowRequestAddDTO dto) {
        return Result.success(groupBorrowRequestService.createRequest(dto));
    }

    @Operation(summary = "群组借阅申请列表")
    @GetMapping("/request")
    public Result<List<GroupBorrowRequestVO>> getRequests(
            @Parameter(description = "群组ID", required = true)
            @RequestParam("group_id") Long groupId) {
        return Result.success(groupBorrowRequestService.getRequests(groupId));
    }

    @Operation(summary = "同意群组借阅申请")
    @PostMapping("/request/{requestId}/approve")
    public Result<GroupBorrowRequestVO> approveRequest(
            @Parameter(description = "借阅申请ID", required = true)
            @PathVariable("requestId") Long requestId) {
        return Result.success(groupBorrowRequestService.approveRequest(requestId));
    }

    @Operation(summary = "拒绝群组借阅申请")
    @PostMapping("/request/{requestId}/reject")
    public Result<GroupBorrowRequestVO> rejectRequest(
            @Parameter(description = "借阅申请ID", required = true)
            @PathVariable("requestId") Long requestId) {
        return Result.success(groupBorrowRequestService.rejectRequest(requestId));
    }
}
