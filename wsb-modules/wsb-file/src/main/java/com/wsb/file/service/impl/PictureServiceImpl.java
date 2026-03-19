package com.wsb.file.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.wsb.common.core.exception.ServiceException;
import com.wsb.file.config.CosConfig;
import com.wsb.file.service.PictureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureServiceImpl implements PictureService {

    private final COSClient cosClient;
    private final CosConfig cosConfig;

    /**
     * 允许的图片格式
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * Content-Type 映射
     */
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    @Override
    public String upload(byte[] fileContent, String originalFilename) {
        // 验证文件格式
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServiceException("不支持的图片格式，仅支持 jpg/png/gif/webp");
        }

        // 生成对象 key: picture/{yyyy}/{MM}/{dd}/{uuid}.{ext}
        String objectKey = generateObjectKey(extension);

        // 上传到 COS
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileContent.length);
        metadata.setContentType(CONTENT_TYPE_MAP.get(extension));

        PutObjectRequest request = new PutObjectRequest(
                cosConfig.getBucket(),
                objectKey,
                new ByteArrayInputStream(fileContent),
                metadata
        );

        cosClient.putObject(request);
        log.info("上传图片成功: {}", objectKey);

        return objectKey;
    }

    @Override
    public ResponseEntity<byte[]> get(String objectKey) {
        // 安全检查：防止路径穿越
        if (objectKey.contains("..") || objectKey.startsWith("/")) {
            throw new ServiceException("非法的图片路径");
        }

        // 从 COS 获取图片
        COSObject cosObject;
        try {
            cosObject = cosClient.getObject(cosConfig.getBucket(), objectKey);
        } catch (Exception e) {
            log.error("获取图片失败: {}", objectKey, e);
            throw new ServiceException("图片不存在");
        }

        // 读取内容
        try (COSObjectInputStream inputStream = cosObject.getObjectContent()) {
            byte[] bytes = inputStream.readAllBytes();

            // 获取 Content-Type
            String contentType = cosObject.getObjectMetadata().getContentType();
            if (contentType == null) {
                contentType = guessContentType(objectKey);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(bytes);
        } catch (IOException e) {
            log.error("读取图片内容失败: {}", objectKey, e);
            throw new ServiceException("读取图片失败");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 生成对象 key
     */
    private String generateObjectKey(String extension) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("picture/%s/%s.%s", datePath, uuid, extension);
    }

    /**
     * 根据 key 猜测 Content-Type
     */
    private String guessContentType(String objectKey) {
        String extension = getExtension(objectKey);
        return CONTENT_TYPE_MAP.getOrDefault(extension, "application/octet-stream");
    }
}