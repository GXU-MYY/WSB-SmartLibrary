package com.wsb.book.job;

import com.wsb.book.service.support.BookBorrowStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookBorrowStatusSyncJob {

    private static final String LOCK_KEY = "book:borrow:sync:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final BookBorrowStatusSyncService syncService;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "${book.borrow.sync-overdue-cron:0 0 0 * * ?}")
    public void syncOverdueBorrows() {
        String owner = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, owner, LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            syncService.syncOverdueBorrows();
        } catch (Exception e) {
            log.error("同步借阅逾期状态失败", e);
        } finally {
            String currentOwner = stringRedisTemplate.opsForValue().get(LOCK_KEY);
            if (owner.equals(currentOwner)) {
                stringRedisTemplate.delete(LOCK_KEY);
            }
        }
    }
}
