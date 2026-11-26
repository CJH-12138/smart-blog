package com.blog.service.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.constant.StudyConstants;
import com.blog.config.ElasticsearchProperties;
import com.blog.config.rag.BlogAiRagProperties;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.entity.StudyCategory;
import com.blog.entity.StudyQuestion;
import com.blog.entity.Tag;
import com.blog.mapper.StudyQuestionMapper;
import com.blog.service.ArticleService;
import com.blog.service.ArticleTagService;
import com.blog.service.CategoryService;
import com.blog.service.TagService;
import com.blog.service.rag.RagContentChunker;
import com.blog.service.rag.RagIndexedChunk;
import com.blog.service.rag.RagIndexingService;
import com.blog.service.rag.RagVectorStoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class RagIndexingServiceImpl implements RagIndexingService {

    private final BlogAiRagProperties properties;
    private final ElasticsearchProperties elasticsearchProperties;
    private final ArticleService articleService;
    private final ArticleTagService articleTagService;
    private final TagService tagService;
    private final CategoryService categoryService;
    private final StudyQuestionMapper studyQuestionMapper;
    private final com.blog.service.StudyCategoryService studyCategoryService;
    private final ObjectProvider<RagVectorStoreService> ragVectorStoreServiceProvider;
    private final ObjectProvider<RestClient> ragElasticsearchRestClientProvider;
    private final ObjectMapper objectMapper;
    private final RagContentChunker chunker;
    private volatile LocalDateTime lastReindexTime;

    public RagIndexingServiceImpl(BlogAiRagProperties properties,
                                  ElasticsearchProperties elasticsearchProperties,
                                  ArticleService articleService,
                                  ArticleTagService articleTagService,
                                  TagService tagService,
                                  CategoryService categoryService,
                                  StudyQuestionMapper studyQuestionMapper,
                                  com.blog.service.StudyCategoryService studyCategoryService,
                                  ObjectProvider<RagVectorStoreService> ragVectorStoreServiceProvider,
                                  ObjectProvider<RestClient> ragElasticsearchRestClientProvider,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.elasticsearchProperties = elasticsearchProperties;
        this.articleService = articleService;
        this.articleTagService = articleTagService;
        this.tagService = tagService;
        this.categoryService = categoryService;
        this.studyQuestionMapper = studyQuestionMapper;
        this.studyCategoryService = studyCategoryService;
        this.ragVectorStoreServiceProvider = ragVectorStoreServiceProvider;
        this.ragElasticsearchRestClientProvider = ragElasticsearchRestClientProvider;
        this.objectMapper = objectMapper;
        this.chunker = new RagContentChunker(properties);
    }

    @Override
    public void upsertArticle(Long articleId) {
        if (!properties.isEnabled() || articleId == null) {
            return;
        }
        RagVectorStoreService ragVectorStoreService = ragVectorStoreServiceProvider.getIfAvailable();
        if (ragVectorStoreService == null) {
            return;
        }
        try {
            Article article = articleService.getById(articleId);
            if (!isSearchableArticle(article)) {
                deleteArticle(articleId);
                return;
            }
            Category category = article.getCategoryId() == null ? null : categoryService.getById(article.getCategoryId());
            article.setCategoryName(category == null ? null : category.getName());
            List<ArticleTag> articleTags = articleTagService.list(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, articleId));
            List<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).filter(Objects::nonNull).distinct().toList();
            List<String> tagNames = tagIds.isEmpty() ? Collections.emptyList() : tagService.listByIds(tagIds).stream()
                    .map(Tag::getName)
                    .filter(StringUtils::hasText)
                    .toList();
            List<RagIndexedChunk> chunks = chunker.chunkArticle(article, tagIds, tagNames);
            ragVectorStoreService.upsertChunks("article", articleId, chunks);
        } catch (Exception e) {
            log.warn("同步 RAG 文章索引失败 articleId={}, reason={}", articleId, e.getMessage());
        }
    }

    @Override
    public void deleteArticle(Long articleId) {
        if (!properties.isEnabled() || articleId == null) {
            return;
        }
        RagVectorStoreService ragVectorStoreService = ragVectorStoreServiceProvider.getIfAvailable();
        if (ragVectorStoreService != null) {
            ragVectorStoreService.deleteBySource("article", articleId);
        }
    }

    @Override
    public void upsertStudyQuestion(Long questionId) {
        if (!properties.isEnabled() || questionId == null) {
            return;
        }
        RagVectorStoreService ragVectorStoreService = ragVectorStoreServiceProvider.getIfAvailable();
        if (ragVectorStoreService == null) {
            return;
        }
        try {
            StudyQuestion question = studyQuestionMapper.selectById(questionId);
            if (!isSearchableStudyQuestion(question)) {
                deleteStudyQuestion(questionId);
                return;
            }
            StudyCategory category = question.getCategoryId() == null ? null : studyCategoryService.getById(question.getCategoryId());
            List<RagIndexedChunk> chunks = chunker.chunkStudyQuestion(question, category == null ? null : category.getCategoryName());
            ragVectorStoreService.upsertChunks("study_question", questionId, chunks);
        } catch (Exception e) {
            log.warn("同步 RAG 题库索引失败 questionId={}, reason={}", questionId, e.getMessage());
        }
    }

    @Override
    public void deleteStudyQuestion(Long questionId) {
        if (!properties.isEnabled() || questionId == null) {
            return;
        }
        RagVectorStoreService ragVectorStoreService = ragVectorStoreServiceProvider.getIfAvailable();
        if (ragVectorStoreService != null) {
            ragVectorStoreService.deleteBySource("study_question", questionId);
        }
    }

    @Override
    public Map<String, Object> rebuild(String scope) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("scope", scope);
        if (!properties.isEnabled()) {
            result.put("success", false);
            result.put("message", "RAG 未启用");
            return result;
        }
        if (ragVectorStoreServiceProvider.getIfAvailable() == null) {
            result.put("success", false);
            result.put("message", "RAG 向量服务未初始化");
            return result;
        }
        String normalizedScope = normalizeScope(scope);
        int articleCount = 0;
        int studyCount = 0;
        if ("all".equals(normalizedScope) || "article".equals(normalizedScope)) {
            List<Article> articles = articleService.list(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
            for (Article article : articles) {
                upsertArticle(article.getId());
                articleCount++;
            }
        }
        if ("all".equals(normalizedScope) || "study".equals(normalizedScope)) {
            List<StudyQuestion> questions = studyQuestionMapper.selectList(new LambdaQueryWrapper<StudyQuestion>()
                    .eq(StudyQuestion::getStatus, StudyConstants.STATUS_ENABLED)
                    .eq(StudyQuestion::getReviewStatus, StudyConstants.REVIEW_PUBLISHED));
            for (StudyQuestion question : questions) {
                upsertStudyQuestion(question.getId());
                studyCount++;
            }
        }
        lastReindexTime = LocalDateTime.now();
        result.put("success", true);
        result.put("articleIndexed", articleCount);
        result.put("studyQuestionIndexed", studyCount);
        result.put("message", "RAG 索引重建完成");
        return result;
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("embeddingProvider", properties.getEmbedding().getProvider());
        result.put("baseUrl", properties.getEmbedding().getBaseUrl());
        result.put("model", properties.getEmbedding().getModel());
        result.put("indexName", properties.getIndexName());
        result.put("lastReindexTime", lastReindexTime);
        result.put("searchEndpoint", elasticsearchProperties.getEndpoint());
        if (!properties.isEnabled()) {
            result.put("available", false);
            result.put("documentCount", 0L);
            result.put("message", "RAG 未启用");
            return result;
        }
        if (ragElasticsearchRestClientProvider.getIfAvailable() == null) {
            result.put("available", false);
            result.put("documentCount", 0L);
            result.put("message", "RAG Elasticsearch 客户端未初始化");
            return result;
        }
        try {
            result.put("documentCount", countDocuments());
            result.put("available", true);
            result.put("message", "RAG 可用");
        } catch (Exception e) {
            result.put("available", false);
            result.put("documentCount", 0L);
            result.put("message", "RAG 不可用，原因: " + e.getMessage());
        }
        return result;
    }

    private long countDocuments() throws IOException {
        RestClient ragElasticsearchRestClient = ragElasticsearchRestClientProvider.getIfAvailable();
        if (ragElasticsearchRestClient == null) {
            return 0L;
        }
        Request request = new Request("GET", "/" + properties.getIndexName() + "/_count");
        Response response = ragElasticsearchRestClient.performRequest(request);
        try (InputStream inputStream = response.getEntity().getContent()) {
            JsonNode root = objectMapper.readTree(inputStream);
            return root.path("count").asLong(0L);
        }
    }

    private boolean isSearchableArticle(Article article) {
        return article != null && Objects.equals(article.getStatus(), 1);
    }

    private boolean isSearchableStudyQuestion(StudyQuestion question) {
        return question != null
                && Objects.equals(question.getStatus(), StudyConstants.STATUS_ENABLED)
                && Objects.equals(question.getReviewStatus(), StudyConstants.REVIEW_PUBLISHED);
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "all";
        }
        String normalized = scope.trim().toLowerCase();
        return List.of("all", "article", "study").contains(normalized) ? normalized : "all";
    }
}
