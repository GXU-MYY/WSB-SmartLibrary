package com.wsb.file.controller;

import com.wsb.common.core.domain.Result;
import com.wsb.file.service.PictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 图片控制器
 */
@Tag(name = "图片管理")
@RestController
@RequestMapping("/v1/picture")
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;

    @Operation(summary = "图片上传")
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("pic") MultipartFile pic) throws IOException {
        String objectKey = pictureService.upload(pic.getBytes(), pic.getOriginalFilename());
        return Result.success(Map.of("pic", objectKey));
    }

    @Operation(summary = "图片获取")
    @GetMapping
    public ResponseEntity<byte[]> get(@RequestParam("pic") String pic) {
        return pictureService.get(pic);
    }
}