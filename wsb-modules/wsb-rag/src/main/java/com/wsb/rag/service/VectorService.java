package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;

import java.util.List;

/**
 * 向量数据库服务接口
 */
public interface VectorService {

    /**
     * 存储书籍向量文档。
     *
     * @param bookId   书籍ID
     * @param content  用于向量化的文本内容
     * @param metadata 书籍元数据
     */
    void storeEmbedding(Long bookId, String content, BookRemoteDTO metadata);

    /**
     * 相似度搜索
     *
     * @param query 查询文本
     * @param limit 返回数量
     * @return 相似书籍ID列表
     */
    List<Long> searchSimilar(String query, int limit);

    /**
     * 根据书籍ID获取相似书籍
     *
     * @param bookId 书籍ID
     * @param limit  返回数量
     * @return 相似书籍ID列表
     */
    List<Long> getSimilarBooks(Long bookId, int limit);

    /**
     * 删除书籍向量
     *
     * @param bookId 书籍ID
     */
    void deleteEmbedding(Long bookId);
}
