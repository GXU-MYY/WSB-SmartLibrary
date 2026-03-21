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
}