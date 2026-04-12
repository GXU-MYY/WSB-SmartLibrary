package com.wsb.book.api.constant;

/**
 * 图书借阅状态常量
 */
public final class BookBorrowStatus {

    private BookBorrowStatus() {
    }

    /**
     * 借阅中
     */
    public static final int BORROWING = 0;

    /**
     * 已归还
     */
    public static final int RETURNED = 1;

    /**
     * 已逾期
     */
    public static final int OVERDUE = 2;
}
