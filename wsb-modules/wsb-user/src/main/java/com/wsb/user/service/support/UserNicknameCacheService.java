package com.wsb.user.service.support;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.user.api.dto.UserNicknameDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNicknameCacheService {

    private static final String USER_NICKNAME_KEY_PREFIX = "user:nickname:";
    private static final String ALL_USER_NICKNAMES_KEY = "user:nicknames:all";
    private static final Duration USER_NICKNAME_TTL = Duration.ofMinutes(30);
    private static final Duration ALL_USER_NICKNAMES_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Map<Long, UserNicknameDTO> getUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Long> uniqueIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }

        List<String> keys = uniqueIds.stream()
                .map(this::buildUserKey)
                .toList();
        List<String> cachedValues = stringRedisTemplate.opsForValue().multiGet(keys);
        if (cachedValues == null || cachedValues.isEmpty()) {
            return Map.of();
        }

        Map<Long, UserNicknameDTO> result = new LinkedHashMap<>();
        for (int i = 0; i < uniqueIds.size(); i++) {
            String cachedJson = cachedValues.get(i);
            if (cachedJson == null) {
                continue;
            }
            try {
                result.put(uniqueIds.get(i), objectMapper.readValue(cachedJson, UserNicknameDTO.class));
            } catch (Exception e) {
                log.warn("读取用户昵称缓存失败: userId={}", uniqueIds.get(i), e);
            }
        }
        return result;
    }

    public void cacheUsers(Collection<UserNicknameDTO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        for (UserNicknameDTO user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            try {
                stringRedisTemplate.opsForValue().set(
                        buildUserKey(user.getId()),
                        objectMapper.writeValueAsString(user),
                        USER_NICKNAME_TTL
                );
            } catch (Exception e) {
                log.warn("写入用户昵称缓存失败: userId={}", user.getId(), e);
            }
        }
    }

    public List<UserNicknameDTO> getAllUsers() {
        String cachedJson = stringRedisTemplate.opsForValue().get(ALL_USER_NICKNAMES_KEY);
        if (cachedJson == null) {
            return null;
        }

        try {
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, UserNicknameDTO.class);
            return objectMapper.readValue(cachedJson, listType);
        } catch (Exception e) {
            log.warn("读取全部用户昵称缓存失败", e);
            return null;
        }
    }

    public void cacheAllUsers(List<UserNicknameDTO> users) {
        if (users == null) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    ALL_USER_NICKNAMES_KEY,
                    objectMapper.writeValueAsString(users),
                    ALL_USER_NICKNAMES_TTL
            );
        } catch (Exception e) {
            log.warn("写入全部用户昵称缓存失败", e);
        }
    }

    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(buildUserKey(userId));
    }

    public void evictAllUsers() {
        stringRedisTemplate.delete(ALL_USER_NICKNAMES_KEY);
    }

    private String buildUserKey(Long userId) {
        return USER_NICKNAME_KEY_PREFIX + userId;
    }
}
