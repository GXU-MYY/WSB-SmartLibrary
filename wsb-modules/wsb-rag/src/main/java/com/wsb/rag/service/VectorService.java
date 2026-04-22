package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;

import java.util.Set;
import java.util.List;

/**
 * 向量数据库服务接口
 */
public interface VectorService {

    /**
     * 存储书籍向量文档。
     *
     * @param bookId   书籍ID
     * @param metadata 书籍元数据
     */
    void storeEmbedding(Long bookId, BookRemoteDTO metadata);

    /**
     * 相似度搜索
     *
     * @param query 查询文本
     * @param limit 返回数量
     * @return 相似书籍ID列表
     */
    List<Long> searchSimilar(String query, int limit);

    /**
     * 相似度搜索，并将候选范围限制在指定图书集合内。
     *
     * @param query        查询文本
     * @param limit        返回数量
     * @param bookIdFilter 允许召回的图书ID集合
     * @return 相似图书ID列表
     */
    List<Long> searchSimilar(String query, int limit, Set<Long> bookIdFilter);

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
