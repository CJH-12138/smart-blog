package com.blog.service.storage;

import com.blog.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "local")
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;
    private final ImageStorageProcessor imageProcessor = new ImageStorageProcessor();
    private Path baseUploadPath;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(storageProperties.getUploadPath());
        if (path.isAbsolute()) {
            baseUploadPath = path;
        } else {
            baseUploadPath = Paths.get(System.getProperty("user.dir"), storageProperties.getUploadPath()).toAbsolutePath();
        }
        Files.createDirectories(baseUploadPath);
        log.info("使用本地文件存储，上传目录: {}", baseUploadPath);
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        StoredFile storedFile = imageProcessor.originalFile(file);
        Path filePath = createTargetPath(storedFile.getExtension());
        Files.write(filePath, storedFile.getContent());
        log.info("文件上传到本地成功: {}", filePath);
        return buildAccessUrl(filePath);
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        StoredFile storedFile = imageProcessor.toStoredFile(file);
        Path filePath = createTargetPath(storedFile.getExtension());
        Files.write(filePath, storedFile.getContent());
        log.info("图片上传到本地成功: {} -> {}", file.getOriginalFilename(), filePath);
        return buildAccessUrl(filePath);
    }

    private Path createTargetPath(String extension) throws IOException {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID() + (extension == null ? "" : extension);
        Path dirPath = baseUploadPath.resolve(datePath);
        Files.createDirectories(dirPath);
        return dirPath.resolve(fileName);
    }

    private String buildAccessUrl(Path filePath) {
        String relativePath = baseUploadPath.relativize(filePath).toString().replace("\\", "/");
        return "/uploads/" + relativePath;
    }
}
