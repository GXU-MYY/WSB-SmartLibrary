package com.wsb.book.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wsb.book.api.dto.BookRemoteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
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
public class BookRemoteCacheService {

    private static final String BOOK_CACHE_KEY_PREFIX = "book:";
    private static final Duration BOOK_CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Map<Long, BookRemoteDTO> getBooks(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Map.of();
        }

        List<Long> uniqueIds = bookIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }

        List<String> cacheKeys = uniqueIds.stream()
                .map(this::buildCacheKey)
                .toList();
        List<String> cachedValues = stringRedisTemplate.opsForValue().multiGet(cacheKeys);
        if (cachedValues == null) {
            return Map.of();
        }

        Map<Long, BookRemoteDTO> result = new LinkedHashMap<>();
        for (int i = 0; i < uniqueIds.size(); i++) {
            String cachedJson = cachedValues.get(i);
            if (cachedJson == null) {
                continue;
            }
            try {
                result.put(uniqueIds.get(i), objectMapper.readValue(cachedJson, BookRemoteDTO.class));
            } catch (Exception e) {
                log.warn("读取图书详情缓存失败: bookId={}", uniqueIds.get(i), e);
            }
        }
        return result;
    }

    public BookRemoteDTO getBook(Long bookId) {
        if (bookId == null) {
            return null;
        }
        return getBooks(List.of(bookId)).get(bookId);
    }

    public void cacheBook(BookRemoteDTO book) {
        if (book == null || book.getId() == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildCacheKey(book.getId()),
                    objectMapper.writeValueAsString(book),
                    BOOK_CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("写入图书详情缓存失败: bookId={}", book.getId(), e);
        }
    }

    public void cacheBooks(Collection<BookRemoteDTO> books) {
        if (books == null || books.isEmpty()) {
            return;
        }

        Map<String, String> payloads = new LinkedHashMap<>();
        for (BookRemoteDTO book : books) {
            if (book == null || book.getId() == null) {
                continue;
            }
            try {
                payloads.put(buildCacheKey(book.getId()), objectMapper.writeValueAsString(book));
            } catch (Exception e) {
                log.warn("缁勮鍥句功缂撳瓨鏁版嵁澶辫触: bookId={}", book.getId(), e);
            }
        }
        if (payloads.isEmpty()) {
            return;
        }

        try {
            var serializer = stringRedisTemplate.getStringSerializer();
            long ttlSeconds = BOOK_CACHE_TTL.toSeconds();
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                payloads.forEach((key, value) -> connection.stringCommands().set(
                        serializer.serialize(key),
                        serializer.serialize(value),
                        Expiration.seconds(ttlSeconds),
                        RedisStringCommands.SetOption.UPSERT
                ));
                return null;
            });
        } catch (Exception e) {
            log.warn("鎵归噺鍐欏叆鍥句功缂撳瓨澶辫触锛屽洖閫€鍒伴€愭潯鍐欏叆", e);
            payloads.keySet().forEach(key -> {
                Long bookId = Long.valueOf(key.substring(BOOK_CACHE_KEY_PREFIX.length()));
                BookRemoteDTO book = books.stream()
                        .filter(item -> item != null && Objects.equals(item.getId(), bookId))
                        .findFirst()
                        .orElse(null);
                cacheBook(book);
            });
        }
    }

    public void evictBook(Long bookId) {
        if (bookId == null) {
            return;
        }
        stringRedisTemplate.delete(buildCacheKey(bookId));
    }

    public void evictBooks(Collection<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return;
        }
        List<String> keys = bookIds.stream()
                .filter(Objects::nonNull)
                .map(this::buildCacheKey)
                .toList();
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String buildCacheKey(Long bookId) {
        return BOOK_CACHE_KEY_PREFIX + bookId;
    }
}
