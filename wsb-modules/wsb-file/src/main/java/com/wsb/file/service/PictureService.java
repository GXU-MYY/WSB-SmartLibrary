package com.wsb.file.service;

import org.springframework.http.ResponseEntity;

/**
 * 图片服务接口
 */
public interface PictureService {

    /**
     * 上传图片
     *
     * @param fileContent 文件内容
     * @param originalFilename 原始文件名
     * @return COS 对象 key
     */
    String upload(byte[] fileContent, String originalFilename);

    /**
     * 获取图片
     *
     * @param objectKey COS 对象 key
     * @return 图片二进制流
     */
    ResponseEntity<byte[]> get(String objectKey);
}