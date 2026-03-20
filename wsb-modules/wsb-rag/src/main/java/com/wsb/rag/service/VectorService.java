package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;

import java.util.List;

/**
 * 向量数据库服务接口
 */
public interface VectorService {

    /**
     * 存储书籍向量
     *
     * @param bookId    书籍ID
     * @param embedding 嵌入向量
     * @param metadata  元数据
     */
    void storeEmbedding(Long bookId, List<Float> embedding, BookRemoteDTO metadata);

    /**
     * 相似度搜索
     *
     * @param queryEmbedding 查询向量
     * @param limit          返回数量
     * @return 相似书籍ID列表
     */
    List<Long> searchSimilar(List<Float> queryEmbedding, int limit);

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