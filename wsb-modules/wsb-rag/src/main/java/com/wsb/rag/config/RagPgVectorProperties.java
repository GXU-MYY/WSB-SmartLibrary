package com.wsb.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.pgvector")
public class RagPgVectorProperties {

    /**
     * PgVector schema name.
     */
    private String schemaName = "public";

    /**
     * PgVector table name.
     */
    private String tableName = "book_embeddings";

    /**
     * Vector dimensions. Keep aligned with the embedding model output.
     */
    private int dimensions = 1024;

    /**
     * Whether Spring AI should create the vector schema/table.
     */
    private boolean initializeSchema = true;

    /**
     * Similarity distance type. Supported values depend on Spring AI.
     */
    private String distanceType = "COSINE_DISTANCE";

    /**
     * Index type. Supported values depend on Spring AI.
     */
    private String indexType = "HNSW";

    /**
     * Batch size when storing documents.
     */
    private int maxDocumentBatchSize = 1000;

    private final Datasource datasource = new Datasource();

    @Data
    public static class Datasource {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
    }
}
