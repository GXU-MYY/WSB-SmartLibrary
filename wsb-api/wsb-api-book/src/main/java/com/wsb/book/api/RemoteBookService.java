package com.wsb.book.api;

import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 书籍远程调用服务
 */
@FeignClient(value = "wsb-book", path = "/v1/inner")
public interface RemoteBookService {

    /**
     * 根据ID获取书籍信息
     */
    @GetMapping("/book/{bookId}")
    Result<BookRemoteDTO> getBookById(@PathVariable("bookId") Long bookId);

    /**
     * 根据ID列表批量获取书籍信息
     */
    @GetMapping("/book/batch")
    Result<List<BookRemoteDTO>> getBooksByIds(@RequestParam("ids") List<Long> bookIds);

    /**
     * 根据ID获取书架信息
     */
    @GetMapping("/shelf/{shelfId}")
    Result<ShelfRemoteDTO> getShelfById(@PathVariable("shelfId") Long shelfId);

    /**
     * 根据ID列表批量获取书架信息
     */
    @GetMapping("/shelf/batch")
    Result<List<ShelfRemoteDTO>> getShelfByIds(@RequestParam("ids") List<Long> shelfIds);
}