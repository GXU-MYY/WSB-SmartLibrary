package com.wsb.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.community.domain.Share;
import com.wsb.community.mapper.ShareMapper;
import com.wsb.community.service.ShareService;
import org.springframework.stereotype.Service;

/**
 * 分享服务实现类
 */
@Service
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {
}