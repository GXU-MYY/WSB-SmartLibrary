package com.wsb.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.community.api.dto.GroupBorrowRequestAddDTO;
import com.wsb.community.api.vo.GroupBorrowRequestVO;
import com.wsb.community.api.vo.GroupPublicBookVO;
import com.wsb.community.api.vo.GroupPublicShelfVO;
import com.wsb.community.domain.GroupBorrowRequest;

import java.util.List;

/**
 * 群组借阅申请服务
 */
public interface GroupBorrowRequestService extends IService<GroupBorrowRequest> {

    List<GroupPublicShelfVO> getPublicShelves(Long groupId);

    List<GroupPublicBookVO> getPublicBooks(Long groupId);

    GroupBorrowRequestVO createRequest(GroupBorrowRequestAddDTO dto);

    List<GroupBorrowRequestVO> getRequests(Long groupId);

    GroupBorrowRequestVO approveRequest(Long requestId);

    GroupBorrowRequestVO rejectRequest(Long requestId);
}
