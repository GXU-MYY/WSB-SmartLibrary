package com.wsb.user.api;

import com.wsb.common.core.domain.Result;
import com.wsb.user.api.dto.UserRemoteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "wsb-user", path = "/v1/admin")
public interface RemoteUserService {

  /**
   * Get user info by username (includes sensitive info like password, internal only)
   */
  @GetMapping("/inner/info/{username}")
  Result<UserRemoteDTO> getUserInfoByUsername(@PathVariable("username") String username);

  /**
   * Check if users exist by user ID list
   */
  @GetMapping("/inner/exists")
  Result<Void> checkUserExists(@RequestParam("user_ids") List<Long> userIds);
}