package com.wsb.rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookEmbeddingMapper {

    int deleteByBookId(@Param("tableName") String tableName, @Param("bookId") String bookId);

    List<BookRankRow> searchKeywordBookRanks(@Param("tableName") String tableName,
                                             @Param("query") String query,
                                             @Param("pattern") String pattern,
                                             @Param("queryPatterns") List<String> queryPatterns,
                                             @Param("bookIds") List<Long> bookIds,
                                             @Param("limit") int limit);

    List<BookRankRow> searchSimilarBookRanks(@Param("tableName") String tableName,
                                             @Param("bookId") Long bookId,
                                             @Param("candidateLimit") int candidateLimit,
                                             @Param("limit") int limit);
}
