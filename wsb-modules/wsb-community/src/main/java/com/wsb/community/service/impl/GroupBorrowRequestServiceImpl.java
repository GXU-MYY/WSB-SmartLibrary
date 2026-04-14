package com.wsb.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.book.api.RemoteBookService;
import com.wsb.book.api.dto.CommunityBorrowCreateDTO;
import com.wsb.book.api.dto.PublicShelfBookDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.book.api.vo.CommunityBorrowFlowVO;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.community.api.constant.GroupBorrowRequestStatus;
import com.wsb.community.api.dto.GroupBorrowRequestAddDTO;
import com.wsb.community.api.vo.GroupBorrowRequestVO;
import com.wsb.community.api.vo.GroupPublicBookVO;
import com.wsb.community.api.vo.GroupPublicShelfVO;
import com.wsb.community.domain.Group;
import com.wsb.community.domain.GroupBorrowRequest;
import com.wsb.community.domain.GroupUser;
import com.wsb.community.mapper.GroupBorrowRequestMapper;
import com.wsb.community.mapper.GroupMapper;
import com.wsb.community.service.GroupBorrowRequestService;
import com.wsb.community.service.GroupUserService;
import com.wsb.user.api.RemoteUserService;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 群组借阅申请服务实现
 */
@Service
@RequiredArgsConstructor
public class GroupBorrowRequestServiceImpl extends ServiceImpl<GroupBorrowRequestMapper, GroupBorrowRequest>
        implements GroupBorrowRequestService {

    private final GroupMapper groupMapper;
    private final GroupUserService groupUserService;
    private final RemoteBookService remoteBookService;
    private final RemoteUserService remoteUserService;

    @Override
    public List<GroupPublicShelfVO> getPublicShelves(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        checkUserInGroup(groupId, currentUserId);

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            return List.of();
        }

        Result<List<ShelfRemoteDTO>> result = remoteBookService.getPublicShelvesByOwners(memberIds);
        List<ShelfRemoteDTO> shelves = result != null && result.getData() != null ? result.getData() : List.of();
        if (shelves.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nicknameMap = getNicknameMap(memberIds);
        return shelves.stream()
                .map(shelf -> {
                    GroupPublicShelfVO vo = new GroupPublicShelfVO();
                    vo.setId(shelf.getId());
                    vo.setShelfName(shelf.getShelfName());
                    vo.setRemark(shelf.getRemark());
                    vo.setOwnerUserId(shelf.getUserId());
                    vo.setOwnerNickname(nicknameMap.get(shelf.getUserId()));
                    return vo;
                })
                .toList();
    }

    @Override
    public List<GroupPublicBookVO> getPublicBooks(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        checkUserInGroup(groupId, currentUserId);

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            return List.of();
        }

        Result<List<PublicShelfBookDTO>> result = remoteBookService.getPublicShelfBooksByOwners(memberIds);
        List<PublicShelfBookDTO> books = result != null && result.getData() != null ? result.getData() : List.of();
        if (books.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nicknameMap = getNicknameMap(memberIds);
        return books.stream()
                .map(book -> {
                    GroupPublicBookVO vo = new GroupPublicBookVO();
                    vo.setShelfId(book.getShelfId());
                    vo.setShelfName(book.getShelfName());
                    vo.setOwnerUserId(book.getOwnerUserId());
                    vo.setOwnerNickname(nicknameMap.get(book.getOwnerUserId()));
                    vo.setBookId(book.getBookId());
                    vo.setTitle(book.getTitle());
                    vo.setAuthor(book.getAuthor());
                    vo.setCoverUrl(book.getCoverUrl());
                    vo.setIsBorrowed(book.getIsBorrowed());
                    vo.setIsLentOut(book.getIsLentOut());
                    vo.setBorrowable(book.getBorrowable());
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBorrowRequestVO createRequest(GroupBorrowRequestAddDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        checkUserInGroup(dto.getGroupId(), currentUserId);

        if (dto.getDueTime() != null && dto.getDueTime().isBefore(LocalDate.now())) {
            throw new ServiceException("预计归还日期不能早于今天");
        }

        PublicShelfBookDTO publicBook = requirePublicBook(dto.getGroupId(), dto.getBookId());
        if (currentUserId.equals(publicBook.getOwnerUserId())) {
            throw new ServiceException("不能借入自己公开的图书");
        }
        if (!Boolean.TRUE.equals(publicBook.getBorrowable())) {
            throw new ServiceException("这本书当前不可借入");
        }

        boolean duplicatedPending = this.exists(Wrappers.<GroupBorrowRequest>lambdaQuery()
                .eq(GroupBorrowRequest::getGroupId, dto.getGroupId())
                .eq(GroupBorrowRequest::getBookId, dto.getBookId())
                .eq(GroupBorrowRequest::getBorrowerUserId, currentUserId)
                .eq(GroupBorrowRequest::getStatus, GroupBorrowRequestStatus.PENDING)
                .eq(GroupBorrowRequest::getIsDeleted, false));
        if (duplicatedPending) {
            throw new ServiceException("你已经发起过这本书的借阅申请");
        }

        Map<Long, String> nicknameMap = getNicknameMap(List.of(publicBook.getOwnerUserId(), currentUserId));
        GroupBorrowRequest request = new GroupBorrowRequest();
        request.setGroupId(dto.getGroupId());
        request.setBookId(publicBook.getBookId());
        request.setBookName(publicBook.getTitle());
        request.setCoverUrl(publicBook.getCoverUrl());
        request.setShelfId(publicBook.getShelfId());
        request.setShelfName(publicBook.getShelfName());
        request.setOwnerUserId(publicBook.getOwnerUserId());
        request.setOwnerNickname(nicknameMap.get(publicBook.getOwnerUserId()));
        request.setBorrowerUserId(currentUserId);
        request.setBorrowerNickname(nicknameMap.get(currentUserId));
        request.setDueTime(dto.getDueTime());
        request.setRequestRemark(dto.getRequestRemark());
        request.setStatus(GroupBorrowRequestStatus.PENDING);
        request.setIsDeleted(false);
        this.save(request);
        return toVO(request);
    }

    @Override
    public List<GroupBorrowRequestVO> getRequests(Long groupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        checkUserInGroup(groupId, currentUserId);

        return this.list(Wrappers.<GroupBorrowRequest>lambdaQuery()
                        .eq(GroupBorrowRequest::getGroupId, groupId)
                        .eq(GroupBorrowRequest::getIsDeleted, false)
                        .orderByAsc(GroupBorrowRequest::getStatus)
                        .orderByDesc(GroupBorrowRequest::getCreateTime)
                        .orderByDesc(GroupBorrowRequest::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBorrowRequestVO approveRequest(Long requestId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        GroupBorrowRequest request = requirePendingRequest(requestId);
        checkUserInGroup(request.getGroupId(), currentUserId);

        if (!currentUserId.equals(request.getOwnerUserId())) {
            throw new ServiceException("只有出借人可以同意借阅申请");
        }

        CommunityBorrowCreateDTO dto = new CommunityBorrowCreateDTO();
        dto.setGroupId(request.getGroupId());
        dto.setRequestId(request.getId());
        dto.setBookId(request.getBookId());
        dto.setOwnerUserId(request.getOwnerUserId());
        dto.setOwnerNickname(request.getOwnerNickname());
        dto.setBorrowerUserId(request.getBorrowerUserId());
        dto.setBorrowerNickname(request.getBorrowerNickname());
        dto.setBorrowTime(LocalDate.now());
        dto.setDueTime(request.getDueTime());

        Result<CommunityBorrowFlowVO> result = remoteBookService.createCommunityBorrowFlow(dto);
        CommunityBorrowFlowVO flow = result != null ? result.getData() : null;
        if (flow == null || flow.getBorrowFlowId() == null) {
            throw new ServiceException("社群借阅流创建失败");
        }

        request.setBorrowFlowId(flow.getBorrowFlowId());
        request.setStatus(GroupBorrowRequestStatus.APPROVED);
        this.updateById(request);

        this.update(Wrappers.<GroupBorrowRequest>lambdaUpdate()
                .eq(GroupBorrowRequest::getBookId, request.getBookId())
                .eq(GroupBorrowRequest::getStatus, GroupBorrowRequestStatus.PENDING)
                .eq(GroupBorrowRequest::getIsDeleted, false)
                .ne(GroupBorrowRequest::getId, request.getId())
                .set(GroupBorrowRequest::getStatus, GroupBorrowRequestStatus.REJECTED));

        return toVO(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBorrowRequestVO rejectRequest(Long requestId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        GroupBorrowRequest request = requirePendingRequest(requestId);
        checkUserInGroup(request.getGroupId(), currentUserId);

        if (!currentUserId.equals(request.getOwnerUserId())) {
            throw new ServiceException("只有出借人可以拒绝借阅申请");
        }

        request.setStatus(GroupBorrowRequestStatus.REJECTED);
        this.updateById(request);
        return toVO(request);
    }

    private void checkUserInGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || Boolean.TRUE.equals(group.getIsDeleted())) {
            throw new ServiceException("群组不存在");
        }
        if (!Objects.equals(group.getOwnerId(), userId) && !groupUserService.isInGroup(groupId, userId)) {
            throw new ServiceException("你不在该群组中");
        }
    }

    private List<Long> getGroupMemberIds(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        return groupUserService.list(Wrappers.<GroupUser>lambdaQuery()
                        .eq(GroupUser::getGroupId, groupId)
                        .eq(GroupUser::getIsDeleted, false))
                .stream()
                .map(GroupUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> {
                    if (group != null && group.getOwnerId() != null && !ids.contains(group.getOwnerId())) {
                        ids.add(0, group.getOwnerId());
                    }
                    return ids;
                }))
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, String> getNicknameMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Result<List<UserNicknameDTO>> result = remoteUserService.getUserNicknamesByIds(userIds);
        List<UserNicknameDTO> users = result != null && result.getData() != null ? result.getData() : List.of();
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(
                        UserNicknameDTO::getId,
                        user -> user.getNickName() != null ? user.getNickName() : String.valueOf(user.getId()),
                        (left, right) -> left
                ));
    }

    private PublicShelfBookDTO requirePublicBook(Long groupId, Long bookId) {
        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            throw new ServiceException("群组中还没有可借阅成员");
        }

        Result<List<PublicShelfBookDTO>> result = remoteBookService.getPublicShelfBooksByOwners(memberIds);
        List<PublicShelfBookDTO> publicBooks = result != null && result.getData() != null ? result.getData() : List.of();
        return publicBooks.stream()
                .filter(item -> Objects.equals(item.getBookId(), bookId))
                .findFirst()
                .orElseThrow(() -> new ServiceException("这本书当前不在群组公开书架中"));
    }

    private GroupBorrowRequest requirePendingRequest(Long requestId) {
        GroupBorrowRequest request = this.getById(requestId);
        if (request == null || Boolean.TRUE.equals(request.getIsDeleted())) {
            throw new ServiceException("借阅申请不存在");
        }
        if (request.getStatus() == null || request.getStatus() != GroupBorrowRequestStatus.PENDING) {
            throw new ServiceException("当前申请状态不支持该操作");
        }
        return request;
    }

    private GroupBorrowRequestVO toVO(GroupBorrowRequest request) {
        GroupBorrowRequestVO vo = new GroupBorrowRequestVO();
        vo.setId(request.getId());
        vo.setGroupId(request.getGroupId());
        vo.setBookId(request.getBookId());
        vo.setBookName(request.getBookName());
        vo.setCoverUrl(request.getCoverUrl());
        vo.setShelfId(request.getShelfId());
        vo.setShelfName(request.getShelfName());
        vo.setOwnerUserId(request.getOwnerUserId());
        vo.setOwnerNickname(request.getOwnerNickname());
        vo.setBorrowerUserId(request.getBorrowerUserId());
        vo.setBorrowerNickname(request.getBorrowerNickname());
        vo.setDueTime(request.getDueTime());
        vo.setRequestRemark(request.getRequestRemark());
        vo.setBorrowFlowId(request.getBorrowFlowId());
        vo.setStatus(request.getStatus());
        vo.setCreateTime(request.getCreateTime());
        return vo;
    }
}
