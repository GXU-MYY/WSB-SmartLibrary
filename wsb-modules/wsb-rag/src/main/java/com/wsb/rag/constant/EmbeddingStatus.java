package com.wsb.rag.constant;

/**
 * 图书向量生成状态常量。
 */
public final class EmbeddingStatus {

    private EmbeddingStatus() {
    }

    /**
     * 待处理。
     */
    public static final int PENDING = 0;

    /**
     * 处理中。
     */
    public static final int PROCESSING = 1;

    /**
     * 已完成。
     */
    public static final int COMPLETED = 2;
}
