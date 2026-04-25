package com.wsb.rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookEmbeddingMapper {

    int deleteByBookId(@Param("tableName") String tableName, @Param("bookId") String bookId);

    List<String> findContentsByBookId(@Param("tableName") String tableName,
                                      @Param("bookId") String bookId,
                                      @Param("identityChunkType") String identityChunkType,
                                      @Param("subjectChunkType") String subjectChunkType,
                                      @Param("summaryChunkType") String summaryChunkType);

    List<BookRankRow> searchKeywordBookRanks(@Param("tableName") String tableName,
                                             @Param("query") String query,
                                             @Param("pattern") String pattern,
                                             @Param("queryPatterns") List<String> queryPatterns,
                                             @Param("bookIds") List<Long> bookIds,
                                             @Param("limit") int limit);
}
