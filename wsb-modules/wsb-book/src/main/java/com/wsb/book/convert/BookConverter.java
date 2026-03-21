package com.wsb.book.convert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wsb.book.api.dto.BookAddDTO;
import com.wsb.book.api.dto.BookUpdateDTO;
import com.wsb.book.api.vo.BookAddVO;
import com.wsb.book.api.vo.BookVO;
import com.wsb.book.api.vo.IsbnBookVO;
import com.wsb.book.api.vo.MyBookVO;
import com.wsb.book.domain.Book;
import com.wsb.book.response.AliyunIsbnResponse;
import com.wsb.book.response.GoogleBooksResponse;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书转换器
 */
@Mapper(componentModel = "spring")
public interface BookConverter {

    Book toBook(BookAddDTO dto);

    BookAddVO toBookAddVO(Book book);

    BookVO toBookVO(Book book);

    MyBookVO toMyBookVO(Book book);

    default Page<BookVO> toVOPage(Page<Book> bookPage) {
        Page<BookVO> voPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        voPage.setRecords(bookPage.getRecords().stream()
                .map(this::toBookVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBookFromDto(BookUpdateDTO dto, @MappingTarget Book book);

    // ==================== ISBN 查询转换 ====================

    /**
     * 阿里云 ISBN 响应转换为 IsbnBookVO
     */
    @Mapping(source = "pubDate", target = "publishDate")
    @Mapping(source = "page", target = "pageCount")
    @Mapping(source = "gist", target = "summary")
    @Mapping(source = "img", target = "coverUrl")
    @Mapping(source = "pubPlace", target = "pubplace")
    @Mapping(source = "cipTxt", target = "cip")
    @Mapping(source = "yinci", target = "impression")
    @Mapping(source = "format", target = "bookFormat")
    @Mapping(source = "genus", target = "clc")
    IsbnBookVO toIsbnBookVO(AliyunIsbnResponse.BookDetail detail);

    /**
     * 阿里云转换后处理：清理关键词
     */
    @AfterMapping
    default void afterAliyunMapping(AliyunIsbnResponse.BookDetail detail, @MappingTarget IsbnBookVO vo) {
        if (detail != null && StringUtils.isNotBlank(detail.getKeyword())) {
            vo.setKeyword(cleanKeyword(detail.getKeyword()));
        }
    }

    /**
     * Google Books 响应转换为 IsbnBookVO
     */
    @Mapping(source = "publishedDate", target = "publishDate")
    @Mapping(source = "description", target = "summary")
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "pageCount", ignore = true)
    @Mapping(target = "keyword", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isbn", ignore = true)
    @Mapping(target = "isbn10", ignore = true)
    IsbnBookVO toIsbnBookVO(GoogleBooksResponse.VolumeInfo volumeInfo);

    /**
     * Google Books 转换后处理：处理列表、封面、ISBN
     */
    @AfterMapping
    default void afterGoogleMapping(GoogleBooksResponse.VolumeInfo volumeInfo, @MappingTarget IsbnBookVO vo) {
        if (volumeInfo == null) {
            return;
        }
        // 作者列表拼接
        vo.setAuthor(joinList(volumeInfo.getAuthors()));
        // 页数转换
        if (volumeInfo.getPageCount() != null) {
            vo.setPageCount(String.valueOf(volumeInfo.getPageCount()));
        }
        // 分类列表拼接
        vo.setKeyword(joinList(volumeInfo.getCategories()));
        // 封面图片
        if (volumeInfo.getImageLinks() != null) {
            vo.setCoverUrl(volumeInfo.getImageLinks().getThumbnail());
        }
        // ISBN 解析
        if (volumeInfo.getIndustryIdentifiers() != null) {
            for (GoogleBooksResponse.IndustryIdentifier identifier : volumeInfo.getIndustryIdentifiers()) {
                if ("ISBN_13".equals(identifier.getType())) {
                    vo.setIsbn(identifier.getIdentifier());
                } else if ("ISBN_10".equals(identifier.getType())) {
                    vo.setIsbn10(identifier.getIdentifier());
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 清理关键词字符串（去除前后竖线）
     */
    default String cleanKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return keyword.replaceAll("^\\|+|\\|+$", "").replace("|", ",");
    }

    /**
     * 将字符串列表用逗号拼接
     */
    default String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }
}