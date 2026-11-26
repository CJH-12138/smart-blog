package com.blog.service.storage;

import com.blog.config.AliyunOssProperties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class OssStorageSupport {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private OssStorageSupport() {
    }

    public static String buildObjectKey(String objectPrefix, LocalDate date, UUID uuid, String extension) {
        String normalizedPrefix = trimSlashes(objectPrefix);
        String datePath = date.format(DATE_PATH_FORMATTER);
        String fileName = uuid + normalizeExtension(extension);
        return normalizedPrefix.isEmpty()
                ? datePath + "/" + fileName
                : normalizedPrefix + "/" + datePath + "/" + fileName;
    }

    public static String buildPublicUrl(String publicBaseUrl, String objectKey) {
        return trimTrailingSlash(publicBaseUrl) + "/" + trimLeadingSlash(objectKey);
    }

    public static void validateProperties(AliyunOssProperties properties) {
        requireText(properties.getEndpoint(), "endpoint");
        requireText(properties.getBucket(), "bucket");
        requireText(properties.getAccessKeyId(), "access-key-id");
        requireText(properties.getAccessKeySecret(), "access-key-secret");
        requireText(properties.getPublicBaseUrl(), "public-base-url");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("阿里云OSS配置缺失: aliyun.oss." + name);
        }
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        return extension.startsWith(".") ? extension : "." + extension;
    }

    private static String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String trimLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("^/+", "");
    }
}
