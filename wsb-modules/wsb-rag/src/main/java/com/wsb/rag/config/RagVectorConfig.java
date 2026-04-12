package com.wsb.rag.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(RagPgVectorProperties.class)
public class RagVectorConfig {

    @Bean
    public DataSource dataSource(RagPgVectorProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getDatasource().getUrl());
        dataSource.setUsername(properties.getDatasource().getUsername());
        dataSource.setPassword(properties.getDatasource().getPassword());
        dataSource.setDriverClassName(properties.getDatasource().getDriverClassName());
        return dataSource;
    }

    @Bean
    public PgVectorStore pgVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            RagPgVectorProperties properties) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(properties.getSchemaName())
                .vectorTableName(properties.getTableName())
                .dimensions(properties.getDimensions())
                .initializeSchema(properties.isInitializeSchema())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(properties.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(properties.getIndexType()))
                .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
                .build();
    }
}
