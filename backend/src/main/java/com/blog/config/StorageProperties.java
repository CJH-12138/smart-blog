package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class StorageProperties {

    private String storage = "oss";

    private String uploadPath = "uploads";
}
