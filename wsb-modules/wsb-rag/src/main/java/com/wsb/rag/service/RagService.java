package com.wsb.rag.service;

import com.wsb.book.api.dto.BookRemoteDTO;

import java.util.List;

/**
 * RAG 服务接口
 */
public interface RagService {

    /**
     * 智能推荐（自然语言查询）
     *
     * @param query 查询文本
     * @param limit 返回数量
     * @return 推荐书籍列表
     */
    List<BookRemoteDTO> recommend(String query, int limit);

    /**
     * 获取相似书籍
     *
     * @param bookId 书籍ID
     * @param limit  返回数量
     * @return 相似书籍列表
     */
    List<BookRemoteDTO> getSimilarBooks(Long bookId, int limit);

    /**
     * 处理新书入库（生成向量和摘要）
     *
     * @param bookId 书籍ID
     */
    void processNewBook(Long bookId);
}