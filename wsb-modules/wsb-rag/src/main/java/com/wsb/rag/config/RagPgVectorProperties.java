package com.wsb.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.pgvector")
public class RagPgVectorProperties {

    /**
     * PgVector schema 名称。
     */
    private String schemaName = "public";

    /**
     * PgVector 表名。
     */
    private String tableName = "book_embeddings";

    /**
     * 向量维度，需要与 embedding 模型输出保持一致。
     */
    private int dimensions = 1024;

    /**
     * 是否由 Spring AI 初始化向量 schema/table。
     */
    private boolean initializeSchema = true;

    /**
     * 相似度距离类型，支持值取决于 Spring AI。
     */
    private String distanceType = "COSINE_DISTANCE";

    /**
     * 索引类型，支持值取决于 Spring AI。
     */
    private String indexType = "HNSW";

    /**
     * 存储文档时的批量大小。
     */
    private int maxDocumentBatchSize = 1000;

    /**
     * 向量召回的最低相似度分数，越低越容易引入噪声。
     */
    private double similarityThreshold = 0.62;

    /**
     * RRF 融合前的最小内部候选数量。
     */
    private int hybridMinCandidates = 60;

    /**
     * 向量召回和关键词召回的候选数量倍数。
     */
    private int hybridCandidateMultiplier = 4;

    /**
     * RRF 排名常量。
     */
    private int rrfRankConstant = 60;

    /**
     * 向量召回在 RRF 中的权重。
     */
    private double vectorScoreWeight = 1.0;

    /**
     * 关键词召回在 RRF 中的权重。
     */
    private double keywordScoreWeight = 0.8;

    private final Datasource datasource = new Datasource();

    @Data
    public static class Datasource {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
    }
}
