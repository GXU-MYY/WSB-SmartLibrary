package com.wsb.community.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.community.api.dto.ShareAddDTO;
import com.wsb.community.api.vo.ShareRecordVO;
import com.wsb.community.api.vo.ShareVO;
import com.wsb.community.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分享控制器
 */
@Tag(name = "分享管理")
@RestController
@RequestMapping("/v1/group/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "分享书籍/书架")
    @PostMapping
    public Result<ShareVO> share(@Valid @RequestBody ShareAddDTO dto) {
        // 根据参数判断分享类型
        if (dto.getBookId() != null) {
            return Result.success(shareService.shareBook(dto));
        } else if (dto.getBookshelfId() != null) {
            return Result.success(shareService.shareShelf(dto));
        } else {
            return Result.error("请指定要分享的书籍或书架");
        }
    }

    @Operation(summary = "群组内分享记录列表")
    @GetMapping
    public Result<List<ShareRecordVO>> getShareRecords(
            @Parameter(description = "群组ID", required = true)
            @RequestParam("group_id") Long groupId,
            @Parameter(description = "分享类型：book 或 bookshelf")
            @RequestParam(value = "share_type", required = false) String shareType) {
        return Result.success(shareService.getShareRecords(groupId, shareType));
    }
}