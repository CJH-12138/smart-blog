package com.blog.service.rag.impl;

import com.blog.config.rag.BlogAiRagProperties;
import com.blog.service.rag.HybridRetrievalChunkHit;
import com.blog.service.rag.RagIndexedChunk;
import com.blog.service.rag.RagTextSegmentMapper;
import com.blog.service.rag.RagVectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "blog.ai.rag", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RagVectorStoreServiceImpl implements RagVectorStoreService {

    private final BlogAiRagProperties properties;
    private final EmbeddingModel ragEmbeddingModel;
    private final EmbeddingStore<TextSegment> ragEmbeddingStore;

    @Override
    public void upsertChunks(String sourceType, Long sourceId, List<RagIndexedChunk> chunks) {
        deleteBySource(sourceType, sourceId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<TextSegment> segments = chunks.stream().map(RagTextSegmentMapper::toSegment).toList();
        List<Embedding> embeddings = ragEmbeddingModel.embedAll(segments).content();
        List<String> ids = chunks.stream().map(RagIndexedChunk::getId).toList();
        ragEmbeddingStore.addAll(ids, embeddings, segments);
    }

    @Override
    public void deleteBySource(String sourceType, Long sourceId) {
        if (!StringUtils.hasText(sourceType) || sourceId == null) {
            return;
        }
        Filter filter = Filter.and(
                new IsEqualTo("sourceType", sourceType),
                new IsEqualTo("sourceId", sourceId)
        );
        try {
            ragEmbeddingStore.removeAll(filter);
        } catch (Exception e) {
            log.warn("删除 RAG 向量文档失败, sourceType={}, sourceId={}, reason={}", sourceType, sourceId, e.getMessage());
        }
    }

    @Override
    public List<HybridRetrievalChunkHit> search(String query, int limit, double minScore) {
        if (!properties.isEnabled() || !StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        Embedding queryEmbedding = ragEmbeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .query(query)
                .queryEmbedding(queryEmbedding)
                .maxResults(limit)
                .minScore(minScore)
                .build();
        return ragEmbeddingStore.search(request).matches().stream()
                .map(match -> mapMatch(match))
                .toList();
    }

    private HybridRetrievalChunkHit mapMatch(EmbeddingMatch<TextSegment> match) {
        return RagTextSegmentMapper.toHit(match.embedded(), match.score());
    }
}
