package com.wsb.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.user.api.dto.UserNicknameDTO;
import com.wsb.user.api.dto.UserRemoteDTO;
import com.wsb.user.domain.User;
import com.wsb.user.api.dto.UserRegisterDTO;
import com.wsb.user.api.dto.UserResetPwdDTO;
import com.wsb.user.api.dto.UserUpdateDTO;
import com.wsb.user.api.vo.UserInfoVO;

import java.util.List;

public interface UserService extends IService<User> {

  UserInfoVO register(UserRegisterDTO dto);

  UserInfoVO updateUserInfo(UserUpdateDTO dto);

  UserRemoteDTO getUserInfoByUsername(String username);

  Page<UserInfoVO> getUserList(Integer page, Integer pageSize, String userName);

  UserInfoVO getUserInfoByUserId(Long userId);

  void resetPassword(UserResetPwdDTO dto);

  void sendCaptcha(String telphone);

  void existsByIds(List<Long> userIds);

  List<UserNicknameDTO> getUserNicknamesByIds(List<Long> userIds);
}