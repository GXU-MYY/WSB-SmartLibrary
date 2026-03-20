package com.wsb.rag.service;

import java.util.List;

/**
 * Embedding 服务接口
 */
public interface EmbeddingService {

    /**
     * 生成文本嵌入向量
     *
     * @param text 文本内容
     * @return 嵌入向量
     */
    List<Float> generateEmbedding(String text);

    /**
     * 批量生成嵌入向量
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    List<List<Float>> generateEmbeddings(List<String> texts);
}