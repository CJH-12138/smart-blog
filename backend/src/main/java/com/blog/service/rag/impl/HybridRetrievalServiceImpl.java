package com.blog.service.rag.impl;

import com.blog.common.result.PageResult;
import com.blog.config.rag.BlogAiRagProperties;
import com.blog.dto.response.SearchAllVO;
import com.blog.dto.response.SearchArticleVO;
import com.blog.dto.response.SearchStudyQuestionVO;
import com.blog.dto.response.TagVO;
import com.blog.dto.response.UserVO;
import com.blog.service.SearchService;
import com.blog.service.rag.HybridRetrievalChunkHit;
import com.blog.service.rag.HybridRetrievalRanker;
import com.blog.service.rag.HybridRetrievalService;
import com.blog.service.rag.RagTextSegmentMapper;
import com.blog.service.rag.RagVectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    private final BlogAiRagProperties properties;
    private final SearchService searchService;
    private final ObjectProvider<RagVectorStoreService> ragVectorStoreServiceProvider;
    private final HybridRetrievalRanker ranker;

    public HybridRetrievalServiceImpl(BlogAiRagProperties properties,
                                      SearchService searchService,
                                      ObjectProvider<RagVectorStoreService> ragVectorStoreServiceProvider) {
        this.properties = properties;
        this.searchService = searchService;
        this.ragVectorStoreServiceProvider = ragVectorStoreServiceProvider;
        this.ranker = new HybridRetrievalRanker(properties);
    }

    @Override
    public SearchAllVO search(String keyword, int pageSize) {
        int limit = Math.max(pageSize, 1);
        SearchAllVO keywordResult = searchService.search(keyword, 1, limit, false);
        RagVectorStoreService ragVectorStoreService = ragVectorStoreServiceProvider.getIfAvailable();
        if (!properties.isEnabled() || !StringUtils.hasText(keyword) || ragVectorStoreService == null) {
            return keywordResult;
        }
        try {
            List<HybridRetrievalChunkHit> keywordHits = buildKeywordHits(keywordResult);
            List<HybridRetrievalChunkHit> vectorHits = ragVectorStoreService.search(
                    keyword,
                    Math.max(limit * 2, properties.getRetrieval().getMaxResults()),
                    safeDouble(properties.getRetrieval().getMinScore(), 0.6D)
            );
            List<HybridRetrievalChunkHit> ranked = ranker.rank(keywordHits, vectorHits, Math.max(limit * 2, limit), keyword);
            return buildHybridResult(keywordResult, ranked, limit);
        } catch (Exception e) {
            log.warn("混合检索失败，已回退关键词搜索: {}", e.getMessage());
            return keywordResult;
        }
    }

    private SearchAllVO buildHybridResult(SearchAllVO keywordResult, List<HybridRetrievalChunkHit> ranked, int limit) {
        Map<Long, SearchArticleVO> articleMap = new LinkedHashMap<>();
        Map<Long, SearchStudyQuestionVO> questionMap = new LinkedHashMap<>();

        for (HybridRetrievalChunkHit hit : ranked) {
            if ("article".equals(hit.getSourceType())) {
                articleMap.computeIfAbsent(hit.getSourceId(), ignored -> toArticle(hit));
                continue;
            }
            if ("study_question".equals(hit.getSourceType())) {
                questionMap.computeIfAbsent(hit.getSourceId(), ignored -> toStudyQuestion(hit));
            }
        }

        SearchAllVO result = new SearchAllVO();
        result.setEngine("hybrid");
        result.setUsers(keywordResult == null || keywordResult.getUsers() == null ? Collections.emptyList() : keywordResult.getUsers());
        result.setArticles(PageResult.of(new ArrayList<>(articleMap.values()).stream().limit(limit).toList(), (long) articleMap.size(), 1, limit));
        result.setStudyQuestions(PageResult.of(new ArrayList<>(questionMap.values()).stream().limit(limit).toList(), (long) questionMap.size(), 1, limit));

        mergeKeywordFallback(keywordResult, result, limit);
        return result;
    }

    private void mergeKeywordFallback(SearchAllVO keywordResult, SearchAllVO result, int limit) {
        if (keywordResult == null) {
            return;
        }
        mergeArticleFallback(result.getArticles(), keywordResult.getArticles(), limit);
        mergeQuestionFallback(result.getStudyQuestions(), keywordResult.getStudyQuestions(), limit);
    }

    private void mergeArticleFallback(PageResult<SearchArticleVO> target, PageResult<SearchArticleVO> fallback, int limit) {
        if (target == null || fallback == null || fallback.getList() == null) {
            return;
        }
        Map<Long, SearchArticleVO> merged = new LinkedHashMap<>();
        if (target.getList() != null) {
            for (SearchArticleVO item : target.getList()) {
                merged.put(item.getId(), item);
            }
        }
        for (SearchArticleVO item : fallback.getList()) {
            merged.putIfAbsent(item.getId(), item);
        }
        target.setList(new ArrayList<>(merged.values()).stream().limit(limit).toList());
        target.setTotal((long) merged.size());
    }

    private void mergeQuestionFallback(PageResult<SearchStudyQuestionVO> target, PageResult<SearchStudyQuestionVO> fallback, int limit) {
        if (target == null || fallback == null || fallback.getList() == null) {
            return;
        }
        Map<Long, SearchStudyQuestionVO> merged = new LinkedHashMap<>();
        if (target.getList() != null) {
            for (SearchStudyQuestionVO item : target.getList()) {
                merged.put(item.getId(), item);
            }
        }
        for (SearchStudyQuestionVO item : fallback.getList()) {
            merged.putIfAbsent(item.getId(), item);
        }
        target.setList(new ArrayList<>(merged.values()).stream().limit(limit).toList());
        target.setTotal((long) merged.size());
    }

    private List<HybridRetrievalChunkHit> buildKeywordHits(SearchAllVO keywordResult) {
        if (keywordResult == null) {
            return Collections.emptyList();
        }
        List<HybridRetrievalChunkHit> hits = new ArrayList<>();
        appendArticleKeywordHits(hits, keywordResult.getArticles() == null ? Collections.emptyList() : keywordResult.getArticles().getList());
        appendQuestionKeywordHits(hits, keywordResult.getStudyQuestions() == null ? Collections.emptyList() : keywordResult.getStudyQuestions().getList());
        return hits;
    }

    private void appendArticleKeywordHits(List<HybridRetrievalChunkHit> target, List<SearchArticleVO> articles) {
        if (articles == null) {
            return;
        }
        for (int i = 0; i < articles.size(); i++) {
            SearchArticleVO item = articles.get(i);
            if (item == null || item.getId() == null) {
                continue;
            }
            double rankScore = Math.max(1D, articles.size() - i);
            double titleScore = StringUtils.hasText(item.getTitleHighlight()) ? 1.2D : 0D;
            double summaryScore = StringUtils.hasText(item.getSummaryHighlight()) ? 0.6D : 0D;
            target.add(HybridRetrievalChunkHit.builder()
                    .sourceType("article")
                    .sourceId(item.getId())
                    .chunkIndex(0)
                    .title(item.getTitle())
                    .content(firstNonBlank(item.getSummaryHighlight(), item.getSummary()))
                    .summary(item.getSummary())
                    .categoryId(item.getCategoryId())
                    .categoryName(item.getCategoryName())
                    .viewCount(item.getViewCount())
                    .likeCount(item.getLikeCount())
                    .commentCount(item.getCommentCount())
                    .favoriteCount(item.getFavoriteCount())
                    .publishTime(item.getPublishTime())
                    .keywordScore(rankScore + titleScore + summaryScore)
                    .build());
        }
    }

    private void appendQuestionKeywordHits(List<HybridRetrievalChunkHit> target, List<SearchStudyQuestionVO> questions) {
        if (questions == null) {
            return;
        }
        for (int i = 0; i < questions.size(); i++) {
            SearchStudyQuestionVO item = questions.get(i);
            if (item == null || item.getId() == null) {
                continue;
            }
            double rankScore = Math.max(1D, questions.size() - i);
            double titleScore = StringUtils.hasText(item.getTitleHighlight()) ? 1.2D : 0D;
            double summaryScore = StringUtils.hasText(item.getAnswerSummaryHighlight()) ? 0.6D : 0D;
            target.add(HybridRetrievalChunkHit.builder()
                    .sourceType("study_question")
                    .sourceId(item.getId())
                    .chunkIndex(0)
                    .title(item.getTitle())
                    .content(firstNonBlank(item.getAnswerSummaryHighlight(), item.getAnswerSummary()))
                    .summary(item.getAnswerSummary())
                    .categoryId(item.getCategoryId())
                    .categoryName(item.getCategoryName())
                    .keywords(item.getKeywords())
                    .difficulty(item.getDifficulty())
                    .studyCount(item.getStudyCount())
                    .viewCount(item.getViewCount())
                    .publishTime(item.getPublishTime())
                    .keywordScore(rankScore + titleScore + summaryScore)
                    .build());
        }
    }

    private SearchArticleVO toArticle(HybridRetrievalChunkHit hit) {
        SearchArticleVO vo = new SearchArticleVO();
        vo.setId(hit.getSourceId());
        vo.setCategoryId(hit.getCategoryId());
        vo.setCategoryName(hit.getCategoryName());
        vo.setTitle(hit.getTitle());
        vo.setSummary(firstNonBlank(hit.getSummary(), hit.getContent()));
        vo.setPublishTime(hit.getPublishTime());
        vo.setViewCount(hit.getViewCount());
        vo.setLikeCount(hit.getLikeCount());
        vo.setCommentCount(hit.getCommentCount());
        vo.setFavoriteCount(hit.getFavoriteCount());
        vo.setTitleHighlight(hit.getKeywordScore() > 0 ? hit.getTitle() : null);
        vo.setSummaryHighlight(hit.getVectorScore() > 0 ? firstNonBlank(hit.getSummary(), hit.getContent()) : null);
        vo.setAuthor(new UserVO());
        vo.setTags(buildTagVos(hit));
        return vo;
    }

    private List<TagVO> buildTagVos(HybridRetrievalChunkHit hit) {
        return RagTextSegmentMapper.splitCsv(hit.getKeywords()).stream()
                .filter(StringUtils::hasText)
                .map(item -> {
                    TagVO vo = new TagVO();
                    vo.setName(item);
                    return vo;
                })
                .toList();
    }

    private SearchStudyQuestionVO toStudyQuestion(HybridRetrievalChunkHit hit) {
        SearchStudyQuestionVO vo = new SearchStudyQuestionVO();
        vo.setId(hit.getSourceId());
        vo.setCategoryId(hit.getCategoryId());
        vo.setCategoryName(hit.getCategoryName());
        vo.setTitle(hit.getTitle());
        vo.setAnswerSummary(firstNonBlank(hit.getSummary(), hit.getContent()));
        vo.setKeywords(hit.getKeywords());
        vo.setDifficulty(hit.getDifficulty());
        vo.setViewCount(hit.getViewCount());
        vo.setStudyCount(hit.getStudyCount());
        vo.setPublishTime(hit.getPublishTime());
        vo.setTitleHighlight(hit.getKeywordScore() > 0 ? hit.getTitle() : null);
        vo.setAnswerSummaryHighlight(hit.getVectorScore() > 0 ? firstNonBlank(hit.getSummary(), hit.getContent()) : null);
        return vo;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
