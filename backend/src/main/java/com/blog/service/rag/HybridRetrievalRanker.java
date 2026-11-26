package com.blog.service.rag;

import com.blog.config.rag.BlogAiRagProperties;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HybridRetrievalRanker {

    private final BlogAiRagProperties properties;

    public HybridRetrievalRanker(BlogAiRagProperties properties) {
        this.properties = properties;
    }

    public List<HybridRetrievalChunkHit> rank(Collection<HybridRetrievalChunkHit> keywordHits,
                                              Collection<HybridRetrievalChunkHit> vectorHits,
                                              int limit) {
        return rank(keywordHits, vectorHits, limit, null);
    }

    public List<HybridRetrievalChunkHit> rank(Collection<HybridRetrievalChunkHit> keywordHits,
                                              Collection<HybridRetrievalChunkHit> vectorHits,
                                              int limit,
                                              String query) {
        Map<String, HybridRetrievalChunkHit> merged = new LinkedHashMap<>();
        mergeInto(merged, keywordHits);
        mergeInto(merged, vectorHits);
        List<HybridRetrievalChunkHit> result = new ArrayList<>(merged.values());
        for (HybridRetrievalChunkHit hit : result) {
            double keywordWeight = safeDouble(properties.getRetrieval().getKeywordWeight(), 0.65D);
            double vectorWeight = safeDouble(properties.getRetrieval().getVectorWeight(), 0.35D);
            double freshnessBoost = calculateFreshnessBoost(hit.getPublishTime());
            double titleBoost = calculateTitleBoost(query, hit.getTitle());
            hit.setFinalScore(keywordWeight * safeDouble(hit.getKeywordScore(), 0D)
                    + vectorWeight * safeDouble(hit.getVectorScore(), 0D)
                    + freshnessBoost
                    + titleBoost);
        }
        result.sort(Comparator.comparing(HybridRetrievalChunkHit::getFinalScore).reversed());
        return result.stream().limit(Math.max(limit, 1)).toList();
    }

    private void mergeInto(Map<String, HybridRetrievalChunkHit> merged, Collection<HybridRetrievalChunkHit> hits) {
        if (hits == null) {
            return;
        }
        for (HybridRetrievalChunkHit hit : hits) {
            if (hit == null) {
                continue;
            }
            String key = hit.getSourceType() + ":" + hit.getSourceId() + ":" + hit.getChunkIndex();
            HybridRetrievalChunkHit existing = merged.get(key);
            if (existing == null) {
                merged.put(key, hit);
                continue;
            }
            existing.setKeywordScore(Math.max(safeDouble(existing.getKeywordScore(), 0D), safeDouble(hit.getKeywordScore(), 0D)));
            existing.setVectorScore(Math.max(safeDouble(existing.getVectorScore(), 0D), safeDouble(hit.getVectorScore(), 0D)));
            if (!StringUtils.hasText(existing.getContent()) && StringUtils.hasText(hit.getContent())) {
                existing.setContent(hit.getContent());
            }
            if (!StringUtils.hasText(existing.getSummary()) && StringUtils.hasText(hit.getSummary())) {
                existing.setSummary(hit.getSummary());
            }
        }
    }

    private double calculateFreshnessBoost(LocalDateTime publishTime) {
        if (publishTime == null) {
            return 0D;
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(publishTime, LocalDateTime.now()));
        if (days <= 7) {
            return 0.25D;
        }
        if (days <= 30) {
            return 0.12D;
        }
        return 0D;
    }

    private double calculateTitleBoost(String query, String title) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(title)) {
            return 0D;
        }
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        if (!StringUtils.hasText(normalizedQuery) || !StringUtils.hasText(normalizedTitle)) {
            return 0D;
        }
        return normalizedTitle.contains(normalizedQuery) ? 0.35D : 0D;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
