package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    private String endpoint;

    private String bucket;

    private String accessKeyId;

    private String accessKeySecret;

    private String publicBaseUrl;

    private String objectPrefix = "uploads";
}
