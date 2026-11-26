package com.blog.service;

import com.blog.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;

    public String uploadFile(MultipartFile file) throws IOException {
        return storageService.uploadFile(file);
    }

    public String uploadImage(MultipartFile file) throws IOException {
        return storageService.uploadImage(file);
    }
}
