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
     * 单次推荐最多生成的查询扩写数量。
     */
    private int maxExpandedQueries = 3;
}
