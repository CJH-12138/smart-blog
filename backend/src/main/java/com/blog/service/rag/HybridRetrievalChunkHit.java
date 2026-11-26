package com.blog.service.rag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HybridRetrievalChunkHit {

    private String sourceType;
    private Long sourceId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private String keywords;
    private Integer difficulty;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer studyCount;
    private LocalDateTime publishTime;
    @Builder.Default
    private Double keywordScore = 0D;
    @Builder.Default
    private Double vectorScore = 0D;
    @Builder.Default
    private Double finalScore = 0D;
}
