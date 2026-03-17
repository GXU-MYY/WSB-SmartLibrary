package com.wsb.community.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 个人分析VO
 */
@Data
public class PersonalStatsVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 我拥有的书籍统计
     */
    private OwnedStats owned;

    /**
     * 我借阅的书籍统计
     */
    private BorrowedStats borrowed;

    /**
     * 我收藏的书籍统计
     */
    private CollectedStats collected;

    @Data
    public static class OwnedStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 书籍总数
         */
        private Integer totalBooks;

        /**
         * 借出未归还数
         */
        private Integer booksLentUnreturned;

        /**
         * 被收藏数
         */
        private Integer booksBeingCollected;

        /**
         * 按分类统计
         */
        private List<CategoryCount> booksByCategory;
    }

    @Data
    public static class BorrowedStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 总借阅数
         */
        private Integer totalBorrowed;

        /**
         * 未归还数
         */
        private Integer unreturned;
    }

    @Data
    public static class CollectedStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 我收藏的书籍数
         */
        private Integer totalCollected;
    }

    @Data
    public static class CategoryCount implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 分类名称
         */
        private String category;

        /**
         * 数量
         */
        private Integer count;
    }
}