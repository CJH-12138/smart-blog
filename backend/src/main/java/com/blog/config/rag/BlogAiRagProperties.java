package com.blog.config.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.ai.rag")
public class BlogAiRagProperties {

    private boolean enabled = false;

    private String indexName = "blog_rag_content";

    private final Embedding embedding = new Embedding();
    private final Retrieval retrieval = new Retrieval();
    private final Chunk chunk = new Chunk();

    @Data
    public static class Embedding {
        private String provider = "openai-compatible";
        private String baseUrl;
        private String apiKey;
        private String model;
        private Integer maxSegmentsPerBatch = 16;
        private Integer maxRetries = 2;
    }

    @Data
    public static class Retrieval {
        private Integer maxResults = 6;
        private Double minScore = 0.60D;
        private Double keywordWeight = 0.65D;
        private Double vectorWeight = 0.35D;
    }

    @Data
    public static class Chunk {
        private Integer articleMaxLength = 800;
        private Integer articleOverlap = 120;
        private Integer questionMaxLength = 600;
    }
}
