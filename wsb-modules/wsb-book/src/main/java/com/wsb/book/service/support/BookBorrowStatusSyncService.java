package com.wsb.book.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.constant.BookBorrowStatus;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.mapper.BookBorrowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookBorrowStatusSyncService {

    private final BookBorrowMapper bookBorrowMapper;

    public void syncOverdueBorrows() {
        LocalDate today = LocalDate.now();

        int overdueRows = bookBorrowMapper.update(null, Wrappers.<BookBorrow>lambdaUpdate()
                .eq(BookBorrow::getIsDeleted, false)
                .eq(BookBorrow::getStatus, BookBorrowStatus.BORROWING)
                .isNotNull(BookBorrow::getDueTime)
                .lt(BookBorrow::getDueTime, today)
                .set(BookBorrow::getStatus, BookBorrowStatus.OVERDUE));

        if (overdueRows > 0) {
            log.info("同步借阅逾期状态完成: overdueRows={}", overdueRows);
        }
    }
}
