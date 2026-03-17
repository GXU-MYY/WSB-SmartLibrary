package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.community.api.dto.ShareAddDTO;
import com.wsb.community.api.vo.ShareRecordVO;
import com.wsb.community.api.vo.ShareVO;
import com.wsb.community.convert.ShareConverter;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.domain.Share;
import com.wsb.community.mapper.ShareMapper;
import com.wsb.community.service.GroupUserService;
import com.wsb.community.service.ShareService;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分享服务实现类
 */
@Service
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {

    private final ShareConverter shareConverter;
    private final GroupUserService groupUserService;
    private final RemoteBookService remoteBookService;
    private final RemoteUserService remoteUserService;

    /**
     * 分享类型：1-图书，2-书架
     */
    private static final int SHARE_TYPE_BOOK = 1;
    private static final int SHARE_TYPE_SHELF = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareVO shareBook(ShareAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 检查用户是否在群组中
        checkUserInGroup(dto.getGroupId(), currentUserId);

        // 验证书籍存在
        Result<BookRemoteDTO> bookResult = remoteBookService.getBookById(dto.getBookId());
        if (bookResult == null || bookResult.getData() == null) {
            throw new ServiceException("书籍不存在");
        }

        // 创建分享记录
        Share share = new Share();
        share.setGroupId(dto.getGroupId());
        share.setTargetId(dto.getBookId());
        share.setShareType(SHARE_TYPE_BOOK);
        share.setShareUserId(currentUserId);
        share.setIsDeleted(false);
        this.save(share);

        return shareConverter.toShareVO(share);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareVO shareShelf(ShareAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 检查用户是否在群组中
        checkUserInGroup(dto.getGroupId(), currentUserId);

        // 验证书架存在
        Result<ShelfRemoteDTO> shelfResult = remoteBookService.getShelfById(dto.getBookshelfId());
        if (shelfResult == null || shelfResult.getData() == null) {
            throw new ServiceException("书架不存在");
        }

        // 创建分享记录
        Share share = new Share();
        share.setGroupId(dto.getGroupId());
        share.setTargetId(dto.getBookshelfId());
        share.setShareType(SHARE_TYPE_SHELF);
        share.setShareUserId(currentUserId);
        share.setIsDeleted(false);
        this.save(share);

        return shareConverter.toShareVO(share);
    }

    @Override
    public List<ShareRecordVO> getShareRecords(Long groupId, String shareType) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 检查用户是否在群组中
        checkUserInGroup(groupId, currentUserId);

        // 构建查询条件
        var wrapper = Wrappers.<Share>lambdaQuery()
                .eq(Share::getGroupId, groupId)
                .eq(Share::getIsDeleted, false)
                .orderByDesc(Share::getCreateTime);

        // 按分享类型过滤
        if (shareType != null && !shareType.isEmpty()) {
            if ("book".equalsIgnoreCase(shareType)) {
                wrapper.eq(Share::getShareType, SHARE_TYPE_BOOK);
            } else if ("bookshelf".equalsIgnoreCase(shareType)) {
                wrapper.eq(Share::getShareType, SHARE_TYPE_SHELF);
            }
        }

        List<Share> shares = this.list(wrapper);
        if (shares.isEmpty()) {
            return List.of();
        }

        // 转换为VO
        List<ShareRecordVO> voList = shareConverter.toShareRecordVOList(shares);

        // 填充分享类型字符串
        voList.forEach(vo -> {
            Share share = shares.stream().filter(s -> s.getId().equals(vo.getId())).findFirst().orElse(null);
            if (share != null) {
                vo.setShareType(share.getShareType() == SHARE_TYPE_BOOK ? "book" : "bookshelf");
            }
        });

        // 批量获取用户昵称
        fillUserNicknames(voList);

        // 批量获取书籍/书架名称
        fillTargetNames(voList);

        return voList;
    }

    /**
     * 检查用户是否在群组中
     */
    private void checkUserInGroup(Long groupId, Long userId) {
        long count = groupUserService.count(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
                .eq(GroupUser::getUserId, userId)
                .eq(GroupUser::getIsDeleted, false));
        if (count == 0) {
            throw new ServiceException(401, "您不在该群组中");
        }
    }

    /**
     * 填充用户昵称
     */
    private void fillUserNicknames(List<ShareRecordVO> voList) {
        List<Long> userIds = voList.stream()
                .map(ShareRecordVO::getSharePerson)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        Result<List<UserNicknameDTO>> result = remoteUserService.getUserNicknamesByIds(userIds);
        if (result == null || result.getData() == null) {
            return;
        }

        Map<Long, String> nicknameMap = result.getData().stream()
                .collect(Collectors.toMap(UserNicknameDTO::getId, UserNicknameDTO::getNickName, (a, b) -> a));

        voList.forEach(vo -> vo.setNickName(nicknameMap.get(vo.getSharePerson())));
    }

    /**
     * 填充目标名称（书籍/书架名称）
     */
    private void fillTargetNames(List<ShareRecordVO> voList) {
        // 分组：书籍和书架
        Map<String, List<ShareRecordVO>> typeMap = voList.stream()
                .collect(Collectors.groupingBy(ShareRecordVO::getShareType));

        // 处理书籍
        List<ShareRecordVO> bookRecords = typeMap.get("book");
        if (bookRecords != null && !bookRecords.isEmpty()) {
            List<Long> bookIds = bookRecords.stream()
                    .map(ShareRecordVO::getTargetId)
                    .distinct()
                    .collect(Collectors.toList());

            Result<List<BookRemoteDTO>> result = remoteBookService.getBooksByIds(bookIds);
            if (result != null && result.getData() != null) {
                Map<Long, String> bookNameMap = result.getData().stream()
                        .collect(Collectors.toMap(BookRemoteDTO::getId, BookRemoteDTO::getTitle, (a, b) -> a));
                bookRecords.forEach(vo -> vo.setName(bookNameMap.get(vo.getTargetId())));
            }
        }

        // 处理书架
        List<ShareRecordVO> shelfRecords = typeMap.get("bookshelf");
        if (shelfRecords != null && !shelfRecords.isEmpty()) {
            List<Long> shelfIds = shelfRecords.stream()
                    .map(ShareRecordVO::getTargetId)
                    .distinct()
                    .collect(Collectors.toList());

            Result<List<ShelfRemoteDTO>> result = remoteBookService.getShelfByIds(shelfIds);
            if (result != null && result.getData() != null) {
                Map<Long, String> shelfNameMap = result.getData().stream()
                        .collect(Collectors.toMap(ShelfRemoteDTO::getId, ShelfRemoteDTO::getShelfName, (a, b) -> a));
                shelfRecords.forEach(vo -> vo.setName(shelfNameMap.get(vo.getTargetId())));
            }
        }
    }
}