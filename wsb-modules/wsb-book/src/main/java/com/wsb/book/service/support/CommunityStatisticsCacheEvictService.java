package com.wsb.book.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommunityStatisticsCacheEvictService {

    private static final String PERSONAL_STATS_KEY_PREFIX = "stats:personal:";
    private static final String BOOK_RANK_KEY = "rank:book";
    private static final String USER_RANK_KEY = "rank:user";

    private final StringRedisTemplate stringRedisTemplate;

    public void evictPersonalStats(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(PERSONAL_STATS_KEY_PREFIX + userId);
    }

    public void evictPersonalStats(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<String> keys = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(userId -> PERSONAL_STATS_KEY_PREFIX + userId)
                .toList();
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    public void evictBookRank() {
        stringRedisTemplate.delete(BOOK_RANK_KEY);
    }

    public void evictUserRank() {
        stringRedisTemplate.delete(USER_RANK_KEY);
    }
}
