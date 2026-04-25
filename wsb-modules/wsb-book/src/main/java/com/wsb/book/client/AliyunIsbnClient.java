package com.wsb.book.client;

import com.wsb.book.response.AliyunIsbnResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 阿里云 ISBN 图书查询 API Feign 客户端
 */
@FeignClient(name = "aliyun-isbn", url = "${aliyun.isbn.base-url}")
public interface AliyunIsbnClient {

    /**
     * 通过 ISBN 查询图书信息
     *
     * @param body          表单数据 (isbn=xxx)
     * @param authorization Authorization Header: APPCODE xxx
     * @return 图书查询结果
     */
    @PostMapping(value = "/isbn/query", consumes = "application/x-www-form-urlencoded")
    AliyunIsbnResponse queryByIsbn(
            @RequestBody String body,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * 构建请求体
     */
    static String buildBody(String isbn) {
        return "isbn=" + isbn;
    }
}