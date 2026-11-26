package com.blog.service.storage;

import com.blog.config.AliyunOssProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OssStorageSupportTest {

    @Test
    void shouldBuildObjectKeyWithPrefixDateUuidAndExtension() {
        String key = OssStorageSupport.buildObjectKey(
                "uploads/",
                LocalDate.of(2026, 4, 25),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ".jpg"
        );

        assertEquals("uploads/2026/04/25/11111111-1111-1111-1111-111111111111.jpg", key);
    }

    @Test
    void shouldBuildPublicUrlWithoutDuplicateSlash() {
        String url = OssStorageSupport.buildPublicUrl(
                "https://cdn.example.com/",
                "uploads/2026/04/25/a.jpg"
        );

        assertEquals("https://cdn.example.com/uploads/2026/04/25/a.jpg", url);
    }

    @Test
    void shouldRejectMissingOssConfiguration() {
        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("bucket");
        properties.setAccessKeyId("ak");
        properties.setPublicBaseUrl("https://cdn.example.com");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> OssStorageSupport.validateProperties(properties)
        );

        assertTrue(ex.getMessage().contains("access-key-secret"));
    }
}
