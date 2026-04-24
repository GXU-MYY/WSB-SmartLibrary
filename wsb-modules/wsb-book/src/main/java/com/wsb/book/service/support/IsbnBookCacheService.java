package com.wsb.book.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.vo.IsbnBookVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsbnBookCacheService {

    private static final String ISBN_CACHE_KEY_PREFIX = "isbn:";
    private static final Duration ISBN_CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public IsbnBookVO get(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn == null) {
            return null;
        }

        String cachedJson = stringRedisTemplate.opsForValue().get(buildCacheKey(normalizedIsbn));
        if (cachedJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedJson, IsbnBookVO.class);
        } catch (Exception e) {
            log.warn("读取 ISBN 缓存失败: isbn={}", normalizedIsbn, e);
            return null;
        }
    }

    public void cache(String isbn, IsbnBookVO book) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn == null || book == null) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    buildCacheKey(normalizedIsbn),
                    objectMapper.writeValueAsString(book),
                    ISBN_CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("写入 ISBN 缓存失败: isbn={}", normalizedIsbn, e);
        }
    }

    private String normalizeIsbn(String isbn) {
        if (StringUtils.isBlank(isbn)) {
            return null;
        }
        String normalized = isbn.replaceAll("[\\s-]", "");
        return StringUtils.isBlank(normalized) ? null : normalized;
    }

    private String buildCacheKey(String isbn) {
        return ISBN_CACHE_KEY_PREFIX + isbn;
    }
}
