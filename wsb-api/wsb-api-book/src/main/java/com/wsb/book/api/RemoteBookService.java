package com.wsb.book.api;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 书籍远程调用服务
 */
@FeignClient(value = "wsb-book", path = "/v1/inner/book")
public interface RemoteBookService {

    /**
     * 根据ID获取书籍信息
     */
    @GetMapping("/{bookId}")
    Result<BookRemoteDTO> getBookById(@PathVariable("bookId") Long bookId);

    /**
     * 根据ID列表批量获取书籍信息
     */
    @GetMapping("/batch")
    Result<List<BookRemoteDTO>> getBooksByIds(List<Long> bookIds);
}