package com.wsb.rag.service;

/**
 * AI 摘要服务接口
 */
public interface SummaryService {

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