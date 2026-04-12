package com.wsb.book.client;

import com.wsb.book.config.GoogleBooksFeignConfig;
import com.wsb.book.response.GoogleBooksResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Google Books API Feign 客户端
 */
@FeignClient(name = "google-books", url = "${google.books.base-url}", configuration = GoogleBooksFeignConfig.class)
public interface GoogleBooksClient {

    /**
     * 通过 ISBN 搜索图书
     *
     * @param query  查询条件，格式: isbn:{isbn}
     * @param apiKey Google Books API Key
     * @return 图书搜索结果
     */
    @GetMapping("/volumes")
    GoogleBooksResponse searchByIsbn(
            @RequestParam("q") String query,
            @RequestParam("key") String apiKey
    );

    /**
     * 根据 volume id 获取图书详情
     *
     * @param volumeId Google Books volume id
     * @param apiKey   Google Books API Key
     * @return 图书详情
     */
    @GetMapping("/volumes/{volumeId}")
    GoogleBooksResponse.BookItem getVolumeById(
            @org.springframework.web.bind.annotation.PathVariable("volumeId") String volumeId,
            @RequestParam("key") String apiKey
    );
}
