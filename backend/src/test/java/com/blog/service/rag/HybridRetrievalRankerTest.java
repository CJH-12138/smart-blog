package com.blog.service.rag;

import com.blog.config.rag.BlogAiRagProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRetrievalRankerTest {

    @Test
    void should_merge_keyword_and_vector_scores_and_deduplicate() {
        BlogAiRagProperties properties = new BlogAiRagProperties();
        properties.getRetrieval().setKeywordWeight(0.65D);
        properties.getRetrieval().setVectorWeight(0.35D);

        HybridRetrievalRanker ranker = new HybridRetrievalRanker(properties);

        HybridRetrievalChunkHit keywordHit = HybridRetrievalChunkHit.builder()
                .sourceType("article")
                .sourceId(10L)
                .chunkIndex(0)
                .title("Redis 缓存击穿")
                .content("缓存击穿解决方案")
                .publishTime(LocalDateTime.now().minusDays(1))
                .keywordScore(9.0D)
                .vectorScore(0.0D)
                .build();

        HybridRetrievalChunkHit vectorHit = HybridRetrievalChunkHit.builder()
                .sourceType("article")
                .sourceId(10L)
                .chunkIndex(0)
                .title("Redis 缓存击穿")
                .content("缓存击穿解决方案")
                .publishTime(LocalDateTime.now().minusDays(1))
                .keywordScore(0.0D)
                .vectorScore(0.9D)
                .build();

        HybridRetrievalChunkHit vectorOnlyHit = HybridRetrievalChunkHit.builder()
                .sourceType("study_question")
                .sourceId(20L)
                .chunkIndex(1)
                .title("JVM CMS 面试题")
                .content("CMS 回收器")
                .publishTime(LocalDateTime.now().minusDays(30))
                .keywordScore(0.0D)
                .vectorScore(0.95D)
                .build();

        List<HybridRetrievalChunkHit> ranked = ranker.rank(List.of(keywordHit), List.of(vectorHit, vectorOnlyHit), 5);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).getSourceId()).isEqualTo(10L);
        assertThat(ranked.get(0).getFinalScore()).isGreaterThan(ranked.get(1).getFinalScore());
        assertThat(ranked.get(0).getKeywordScore()).isEqualTo(9.0D);
        assertThat(ranked.get(0).getVectorScore()).isEqualTo(0.9D);
    }

    @Test
    void should_boost_title_matches_when_scores_are_close() {
        BlogAiRagProperties properties = new BlogAiRagProperties();
        HybridRetrievalRanker ranker = new HybridRetrievalRanker(properties);

        HybridRetrievalChunkHit titleMatched = HybridRetrievalChunkHit.builder()
                .sourceType("article")
                .sourceId(1L)
                .chunkIndex(0)
                .title("JVM 调优指南")
                .content("主要讲解垃圾回收")
                .keywordScore(1.2D)
                .vectorScore(0.72D)
                .build();

        HybridRetrievalChunkHit noTitleMatch = HybridRetrievalChunkHit.builder()
                .sourceType("article")
                .sourceId(2L)
                .chunkIndex(0)
                .title("Java 并发指南")
                .content("也提到了 JVM")
                .keywordScore(1.2D)
                .vectorScore(0.72D)
                .build();

        List<HybridRetrievalChunkHit> ranked = ranker.rank(List.of(titleMatched, noTitleMatch), List.of(), 5, "JVM");

        assertThat(ranked.get(0).getSourceId()).isEqualTo(1L);
    }
}
