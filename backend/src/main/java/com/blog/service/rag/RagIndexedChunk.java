package com.blog.service.rag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class RagIndexedChunk {

    private String id;
    private String sourceType;
    private Long sourceId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private String categoryName;
    @Builder.Default
    private List<Long> tagIds = new ArrayList<>();
    @Builder.Default
    private List<String> tagNames = new ArrayList<>();
    private Long authorId;
    private Integer status;
    private String keywords;
    private Integer difficulty;
    private LocalDateTime publishTime;
    private LocalDateTime updatedTime;
}
