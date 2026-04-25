package com.wsb.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.common.core.utils.SmsUtils;
import com.wsb.user.api.dto.UserLoginDTO;
import com.wsb.user.api.dto.UserNicknameDTO;
import com.wsb.user.api.dto.UserRegisterDTO;
import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.api.dto.UserResetPwdDTO;
import com.wsb.user.api.dto.UserUpdateDTO;
import com.wsb.user.api.vo.UserInfoVO;
import com.wsb.user.convert.UserConverter;
import com.wsb.user.domain.User;
import com.wsb.user.mapper.UserMapper;
import com.wsb.user.service.UserService;
import com.wsb.user.service.support.UserNicknameCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserConverter userConverter;
    private final SmsUtils smsUtils;
    private final UserNicknameCacheService userNicknameCacheService;

    @Override
    public UserInfoVO register(UserRegisterDTO dto) {
        boolean ok = smsUtils.checkVerifyCode(dto.getPhone(), dto.getCaptcha());
        if (!ok) {
            throw new ServiceException("验证码错误或已过期");
        }

        long count = this.count(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (count > 0) {
            throw new ServiceException("该手机号已注册");
        }

        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setUserName(dto.getPhone());
        user.setNickName("用户" + RandomUtil.randomString(6));
        user.setIsActive(true);
        user.setIsConfirmed(true);
        user.setIsDeleted(false);
        this.save(user);

        userNicknameCacheService.evictAllUsers();
        return userConverter.toUserInfoVO(user);
    }

    @Override
    public UserInfoVO updateUserInfo(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = this.getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        String nickName = trimToNull(dto.getNickName());
        String realName = trimToNull(dto.getRealName());
        String email = trimToNull(dto.getEmail());
        String signature = trimToNull(dto.getSignature());
        String avatar = trimToNull(dto.getAvatar());

        validateEmail(userId, email, user.getEmail());

        user.setNickName(nickName);
        user.setRealName(realName);
        user.setEmail(email);
        user.setSignature(signature);
        user.setAvatar(avatar);
        this.updateById(user);

        userNicknameCacheService.evictUser(userId);
        userNicknameCacheService.evictAllUsers();
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
    public Page<UserInfoVO> getUserList(Integer page, Integer pageSize, String userName, String phone) {
        Page<User> userPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userName)) {
            wrapper.like(User::getUserName, userName);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.eq(User::getPhone, phone.trim());
        }
        wrapper.eq(User::getIsDeleted, false);

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
        boolean ok = smsUtils.checkVerifyCode(dto.getPhone(), dto.getCaptcha());
        if (!ok) {
            throw new ServiceException("验证码错误或已过期");
        }

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new ServiceException("该手机号未注册");
        }

        user.setPassword(BCrypt.hashpw(dto.getPasswd(), BCrypt.gensalt()));
        this.updateById(user);
    }

    @Override
    public void sendCaptcha(String phone) {
        String verifyCode = smsUtils.sendSmsVerifyCode(phone);
        log.info("发送短信验证码 {} 到 {}", verifyCode, phone);
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

        List<Long> uniqueIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        Map<Long, UserNicknameDTO> nicknameMap = new LinkedHashMap<>(userNicknameCacheService.getUsers(uniqueIds));
        List<Long> missIds = uniqueIds.stream()
                .filter(userId -> !nicknameMap.containsKey(userId))
                .toList();

        if (!missIds.isEmpty()) {
            List<UserNicknameDTO> loadedUsers = this.listByIds(missIds).stream()
                    .filter(Objects::nonNull)
                    .map(this::toUserNicknameDTO)
                    .collect(Collectors.toList());
            userNicknameCacheService.cacheUsers(loadedUsers);
            loadedUsers.forEach(user -> nicknameMap.put(user.getId(), user));
        }

        return uniqueIds.stream()
                .map(nicknameMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<UserNicknameDTO> getAllUserNicknames() {
        List<UserNicknameDTO> cachedUsers = userNicknameCacheService.getAllUsers();
        if (cachedUsers != null) {
            return cachedUsers;
        }

        List<UserNicknameDTO> users = this.list().stream()
                .map(this::toUserNicknameDTO)
                .collect(Collectors.toList());
        userNicknameCacheService.cacheUsers(users);
        userNicknameCacheService.cacheAllUsers(users);
        return users;
    }

    @Override
    public SaTokenInfo login(UserLoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, dto.getUsername()));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new ServiceException("密码错误");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ServiceException("账号未激活");
        }

        StpUtil.login(user.getId());
        return StpUtil.getTokenInfo();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private void validateEmail(Long userId, String newEmail, String currentEmail) {
        if (!StringUtils.hasText(newEmail) || Objects.equals(newEmail, currentEmail)) {
            return;
        }

        long emailCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, newEmail)
                .ne(User::getId, userId));
        if (emailCount > 0) {
            throw new ServiceException("该邮箱已被其他用户使用");
        }
    }

    private UserNicknameDTO toUserNicknameDTO(User user) {
        UserNicknameDTO dto = new UserNicknameDTO();
        dto.setId(user.getId());
        dto.setNickName(user.getNickName());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
