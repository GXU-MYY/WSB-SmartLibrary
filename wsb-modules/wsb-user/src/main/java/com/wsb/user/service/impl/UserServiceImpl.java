package com.wsb.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.domain.Result;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.user.api.dto.UserLoginDTO;
import com.wsb.user.api.dto.UserNicknameDTO;
import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.convert.UserConverter;
import com.wsb.user.domain.User;
import com.wsb.user.api.dto.UserRegisterDTO;
import com.wsb.user.api.dto.UserResetPwdDTO;
import com.wsb.user.api.dto.UserUpdateDTO;
import com.wsb.user.mapper.UserMapper;
import com.wsb.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wsb.common.core.utils.SmsUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserConverter userConverter;
    private final SmsUtils smsUtils;
    private final StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:phone:";

    @Override
    public UserInfoVO register(UserRegisterDTO dto) {
        // 验证验证码
        boolean ok = smsUtils.checkVerifyCode(dto.getPhone(), dto.getCaptcha());
        if (!ok) {
            throw new ServiceException("验证码错误或已过期");
        }

        // 检查手机号是否已存在
        long count = this.count(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (count > 0) {
            throw new ServiceException("该手机号已注册");
        }

        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt())); // BCrypt加密
        user.setUserName(dto.getPhone()); // 默认用户名为手机号
        user.setNickName("用户" + RandomUtil.randomString(6));
        user.setIsActive(true); // 已激活
        user.setIsConfirmed(true); // 已确认
        user.setIsDeleted(false);
        this.save(user);
        return userConverter.toUserInfoVO(user);
    }

    @Override
    public UserInfoVO updateUserInfo(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = this.getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        userConverter.updateUserFromDto(dto, user);
        this.updateById(user);
        return userConverter.toUserInfoVO(user);
    }

    @Override
    public UserRemoteDTO getUserInfoByUsername(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return userConverter.toUserRemoteDTO(user);
    }

    @Override
    public Page<UserInfoVO> getUserList(Integer page, Integer pageSize, String userName) {
        // 列表查询
        Page<User> userPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(User::getUserName, userName);
        }

        this.page(userPage, wrapper);

        return (Page<UserInfoVO>) userPage.convert(userConverter::toUserInfoVO);
    }

    @Override
    public UserInfoVO getUserInfoByUserId(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return userConverter.toUserInfoVO(user);
    }

    @Override
    public void resetPassword(UserResetPwdDTO dto) {
        // 验证验证码
        boolean ok = smsUtils.checkVerifyCode(dto.getPhone(), dto.getCaptcha());
        if (!ok) {
            throw new ServiceException("验证码错误或已过期");
        }

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new ServiceException("该手机号未注册");
        }

        user.setPassword(BCrypt.hashpw(dto.getPasswd(), BCrypt.gensalt())); // BCrypt加密
        this.updateById(user);
    }

    @Override
    public void sendCaptcha(String phone) {
        // 发送短信
        String verifyCode = smsUtils.sendSmsVerifyCode(phone);

        log.info("发送短信验证码{}到{}", verifyCode, phone);
    }

    @Override
    public void existsByIds(List<Long> userIds) {
        long count = this.count(new LambdaQueryWrapper<User>().in(User::getId, userIds));
        if (count != userIds.size()) {
            throw new ServiceException("部分被邀请用户不存在，请检查后重试");
        }
    }

    @Override
    public List<UserNicknameDTO> getUserNicknamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = this.listByIds(userIds);
        return users.stream().map(user -> {
            UserNicknameDTO dto = new UserNicknameDTO();
            dto.setId(user.getId());
            dto.setNickName(user.getNickName());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserNicknameDTO> getAllUserNicknames() {
        List<User> users = this.list();
        return users.stream().map(user -> {
            UserNicknameDTO dto = new UserNicknameDTO();
            dto.setId(user.getId());
            dto.setNickName(user.getNickName());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public SaTokenInfo login(UserLoginDTO dto) {
        // 1. 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, dto.getUsername()));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException("密码错误");
        }

        // 3. 校验状态
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ServiceException("账号未激活");
        }

        // 4. 登录
        StpUtil.login(user.getId());
        return StpUtil.getTokenInfo();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}