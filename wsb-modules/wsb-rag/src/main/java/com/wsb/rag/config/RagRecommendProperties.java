package com.wsb.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.recommend")
public class RagRecommendProperties {

    /**
     * 最终重排序前拉取的最小候选数量。
     */
    private int minCandidates = 30;

    /**
     * 最终重排序前拉取候选的数量倍数。
     */
    private int candidateMultiplier = 3;

    /**
     * 是否使用已配置的对话模型进行重排序。
     */
    private boolean llmRerankEnabled = true;

    /**
     * 发送给对话模型的最大候选数量。
     */
    private int llmRerankMaxCandidates = 20;

    /**
     * 重排序使用的对话模型。
     */
    private String rerankModel = "deepseek-chat";

    /**
     * 重排序调用超时时间，单位秒。
     */
    private long rerankTimeoutSeconds = 20;
}
