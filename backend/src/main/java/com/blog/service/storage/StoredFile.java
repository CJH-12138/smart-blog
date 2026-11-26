package com.blog.service.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoredFile {

    private final byte[] content;

    private final String extension;

    private final String contentType;
}
