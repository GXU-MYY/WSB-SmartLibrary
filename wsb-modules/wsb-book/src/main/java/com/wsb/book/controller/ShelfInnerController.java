package com.wsb.book.controller;

import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.book.domain.Shelf;
import com.wsb.book.service.ShelfService;
import com.wsb.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 书架内部接口（供其他服务调用）
 */
@RestController
@RequestMapping("/v1/inner/shelf")
@RequiredArgsConstructor
public class ShelfInnerController {

    private final ShelfService shelfService;

    @GetMapping("/{shelfId}")
    public Result<ShelfRemoteDTO> getShelfById(@PathVariable Long shelfId) {
        Shelf shelf = shelfService.getById(shelfId);
        if (shelf == null) {
            return Result.success(null);
        }
        ShelfRemoteDTO dto = new ShelfRemoteDTO();
        dto.setId(shelf.getId());
        dto.setShelfName(shelf.getShelfName());
        dto.setUserId(shelf.getUserId());
        return Result.success(dto);
    }

    @GetMapping("/batch")
    public Result<List<ShelfRemoteDTO>> getShelfByIds(@RequestParam(value = "ids", required = false) List<Long> shelfIds) {
        if (shelfIds == null || shelfIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Shelf> shelves = shelfService.listByIds(shelfIds);
        List<ShelfRemoteDTO> dtos = shelves.stream().map(shelf -> {
            ShelfRemoteDTO dto = new ShelfRemoteDTO();
            dto.setId(shelf.getId());
            dto.setShelfName(shelf.getShelfName());
            dto.setUserId(shelf.getUserId());
            return dto;
        }).collect(Collectors.toList());
        return Result.success(dtos);
    }
}