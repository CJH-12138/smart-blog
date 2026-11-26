package com.blog.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.blog.config.AliyunOssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "oss", matchIfMissing = true)
public class OssStorageService implements StorageService {

    private final AliyunOssProperties ossProperties;
    private final ImageStorageProcessor imageProcessor = new ImageStorageProcessor();
    private OSS ossClient;

    public OssStorageService(AliyunOssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @PostConstruct
    public void init() {
        OssStorageSupport.validateProperties(ossProperties);
        ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
        log.info("使用阿里云OSS文件存储，bucket: {}, endpoint: {}", ossProperties.getBucket(), ossProperties.getEndpoint());
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadStoredFile(imageProcessor.originalFile(file));
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        return uploadStoredFile(imageProcessor.toStoredFile(file));
    }

    private String uploadStoredFile(StoredFile storedFile) {
        String objectKey = OssStorageSupport.buildObjectKey(
                ossProperties.getObjectPrefix(),
                LocalDate.now(),
                UUID.randomUUID(),
                storedFile.getExtension()
        );

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(storedFile.getContent().length);
        if (storedFile.getContentType() != null && !storedFile.getContentType().isBlank()) {
            metadata.setContentType(storedFile.getContentType());
        }

        ossClient.putObject(
                ossProperties.getBucket(),
                objectKey,
                new ByteArrayInputStream(storedFile.getContent()),
                metadata
        );
        String url = OssStorageSupport.buildPublicUrl(ossProperties.getPublicBaseUrl(), objectKey);
        log.info("文件上传到OSS成功: {}", url);
        return url;
    }
}
