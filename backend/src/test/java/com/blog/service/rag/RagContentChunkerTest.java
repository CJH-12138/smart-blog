package com.blog.service.rag;

import com.blog.config.rag.BlogAiRagProperties;
import com.blog.entity.Article;
import com.blog.entity.StudyQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagContentChunkerTest {

    @Test
    void should_chunk_article_and_clean_html() {
        BlogAiRagProperties properties = new BlogAiRagProperties();
        properties.getChunk().setArticleMaxLength(40);
        properties.getChunk().setArticleOverlap(8);

        RagContentChunker chunker = new RagContentChunker(properties);

        Article article = new Article();
        article.setId(100L);
        article.setUserId(8L);
        article.setCategoryId(2L);
        article.setCategoryName("Java");
        article.setTitle("Redis 缓存实战");
        article.setSummary("讲解缓存穿透和缓存击穿");
        article.setContentHtml("<h1>Redis</h1><p>缓存穿透需要布隆过滤器。</p><p>缓存击穿可以使用互斥锁。</p>");
        article.setStatus(1);
        article.setPublishTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        article.setUpdateTime(LocalDateTime.of(2026, 4, 2, 11, 0));

        List<RagIndexedChunk> chunks = chunker.chunkArticle(article, List.of(11L, 12L), List.of("Redis", "缓存"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(item -> item.getSourceType().equals("article"));
        assertThat(chunks).allMatch(item -> item.getSourceId().equals(100L));
        assertThat(chunks).allMatch(item -> item.getCategoryName().equals("Java"));
        assertThat(chunks).allMatch(item -> item.getTagNames().contains("Redis"));
        assertThat(chunks).noneMatch(item -> item.getContent().contains("<p>"));
        assertThat(chunks.get(0).getTitle()).isEqualTo("Redis 缓存实战");
    }

    @Test
    void should_chunk_question_with_title_and_answer() {
        BlogAiRagProperties properties = new BlogAiRagProperties();
        properties.getChunk().setQuestionMaxLength(60);

        RagContentChunker chunker = new RagContentChunker(properties);

        StudyQuestion question = new StudyQuestion();
        question.setId(200L);
        question.setCategoryId(3L);
        question.setTitle("JVM 中什么是 CMS");
        question.setQuestionStem("请解释 CMS 垃圾回收器的核心思路。");
        question.setStandardAnswer("CMS 以获取最短停顿为目标，分为初始标记、并发标记、重新标记和并发清除。");
        question.setAnswerSummary("低停顿老年代回收器");
        question.setKeywords("JVM,CMS,GC");
        question.setDifficulty(2);
        question.setStatus(1);
        question.setReviewStatus(1);
        question.setPublishTime(LocalDateTime.of(2026, 4, 1, 8, 0));
        question.setUpdateTime(LocalDateTime.of(2026, 4, 1, 9, 0));

        List<RagIndexedChunk> chunks = chunker.chunkStudyQuestion(question, "JVM");

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(item -> item.getSourceType().equals("study_question"));
        assertThat(chunks).allMatch(item -> item.getSourceId().equals(200L));
        assertThat(chunks).allMatch(item -> item.getCategoryName().equals("JVM"));
        assertThat(chunks.get(0).getContent()).contains("CMS");
        assertThat(chunks.get(0).getKeywords()).contains("JVM");
    }
}
