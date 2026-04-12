package com.wsb.rag.service;

/**
 * 图书 AI 内容服务接口
 */
public interface BookAiContentService {

    /**
     * 获取已缓存的网络书评
     *
     * @param bookId 书籍ID
     * @return 聚合书评缓存
     */
    String getCachedReviewDigest(Long bookId);

    /**
     * 生成书籍摘要
     *
     * @param bookId 书籍ID
     * @return 摘要内容
     */
    String generateSummary(Long bookId);

    /**
     * 聚合网络书评
     *
     * @param bookId 书籍ID
     * @param title  书名
     * @param author 作者
     * @return 聚合书评
     */
    String aggregateReviews(Long bookId, String title, String author);
}
