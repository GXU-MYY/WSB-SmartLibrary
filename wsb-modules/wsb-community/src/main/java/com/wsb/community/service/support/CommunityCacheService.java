package com.wsb.community.service.support;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.community.api.vo.BookRankVO;
import com.wsb.community.api.vo.GroupPublicBookVO;
import com.wsb.community.api.vo.GroupPublicShelfVO;
import com.wsb.community.api.vo.PersonalStatsVO;
import com.wsb.community.api.vo.UserRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCacheService {

    private static final String PERSONAL_STATS_KEY_PREFIX = "stats:personal:";
    private static final String BOOK_RANK_KEY = "rank:book";
    private static final String USER_RANK_KEY = "rank:user";
    private static final String GROUP_PUBLIC_SHELVES_KEY_PREFIX = "group:public:shelves:";
    private static final String GROUP_PUBLIC_BOOKS_KEY_PREFIX = "group:public:books:";

    private static final Duration PERSONAL_STATS_TTL = Duration.ofMinutes(3);
    private static final Duration RANK_TTL = Duration.ofMinutes(15);
    private static final Duration GROUP_PUBLIC_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PersonalStatsVO getPersonalStats(Long userId) {
        if (userId == null) {
            return null;
        }
        return readObject(PERSONAL_STATS_KEY_PREFIX + userId, PersonalStatsVO.class);
    }

    public void cachePersonalStats(Long userId, PersonalStatsVO value) {
        if (userId == null || value == null) {
            return;
        }
        writeObject(PERSONAL_STATS_KEY_PREFIX + userId, value, PERSONAL_STATS_TTL);
    }

    public void evictPersonalStats(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(PERSONAL_STATS_KEY_PREFIX + userId);
    }

    public List<BookRankVO> getBookRanks() {
        return readList(BOOK_RANK_KEY, BookRankVO.class);
    }

    public void cacheBookRanks(List<BookRankVO> value) {
        if (value == null) {
            return;
        }
        writeObject(BOOK_RANK_KEY, value, RANK_TTL);
    }

    public void evictBookRanks() {
        stringRedisTemplate.delete(BOOK_RANK_KEY);
    }

    public List<UserRankVO> getUserRanks() {
        return readList(USER_RANK_KEY, UserRankVO.class);
    }

    public void cacheUserRanks(List<UserRankVO> value) {
        if (value == null) {
            return;
        }
        writeObject(USER_RANK_KEY, value, RANK_TTL);
    }

    public void evictUserRanks() {
        stringRedisTemplate.delete(USER_RANK_KEY);
    }

    public List<GroupPublicShelfVO> getGroupPublicShelves(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return readList(GROUP_PUBLIC_SHELVES_KEY_PREFIX + groupId, GroupPublicShelfVO.class);
    }

    public void cacheGroupPublicShelves(Long groupId, List<GroupPublicShelfVO> value) {
        if (groupId == null || value == null || value.isEmpty()) {
            return;
        }
        writeObject(GROUP_PUBLIC_SHELVES_KEY_PREFIX + groupId, value, GROUP_PUBLIC_TTL);
    }

    public List<GroupPublicBookVO> getGroupPublicBooks(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return readList(GROUP_PUBLIC_BOOKS_KEY_PREFIX + groupId, GroupPublicBookVO.class);
    }

    public void cacheGroupPublicBooks(Long groupId, List<GroupPublicBookVO> value) {
        if (groupId == null || value == null || value.isEmpty()) {
            return;
        }
        writeObject(GROUP_PUBLIC_BOOKS_KEY_PREFIX + groupId, value, GROUP_PUBLIC_TTL);
    }

    public void evictGroupPublicShelves(Long groupId) {
        if (groupId == null) {
            return;
        }
        stringRedisTemplate.delete(GROUP_PUBLIC_SHELVES_KEY_PREFIX + groupId);
    }

    public void evictGroupPublicBooks(Long groupId) {
        if (groupId == null) {
            return;
        }
        stringRedisTemplate.delete(GROUP_PUBLIC_BOOKS_KEY_PREFIX + groupId);
    }

    private <T> T readObject(String key, Class<T> type) {
        String cachedJson = stringRedisTemplate.opsForValue().get(key);
        if (cachedJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, type);
        } catch (Exception e) {
            log.warn("读取社区缓存失败: key={}", key, e);
            return null;
        }
    }

    private <T> List<T> readList(String key, Class<T> elementType) {
        String cachedJson = stringRedisTemplate.opsForValue().get(key);
        if (cachedJson == null) {
            return null;
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(cachedJson, listType);
        } catch (Exception e) {
            log.warn("读取社区列表缓存失败: key={}", key, e);
            return null;
        }
    }

    private void writeObject(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("写入社区缓存失败: key={}", key, e);
        }
    }
}
