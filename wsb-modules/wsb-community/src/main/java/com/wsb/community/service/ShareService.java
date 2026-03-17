package com.wsb.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.community.api.dto.ShareAddDTO;
import com.wsb.community.api.vo.ShareRecordVO;
import com.wsb.community.api.vo.ShareVO;
import com.wsb.community.domain.Share;

import java.util.List;

/**
 * 分享服务接口
 */
public interface ShareService extends IService<Share> {

    /**
     * 分享书籍到群组
     */
    ShareVO shareBook(ShareAddDTO dto);

    /**
     * 分享书架到群组
     */
    ShareVO shareShelf(ShareAddDTO dto);

    /**
     * 获取群组内分享记录列表
     */
    List<ShareRecordVO> getShareRecords(Long groupId, String shareType);
}