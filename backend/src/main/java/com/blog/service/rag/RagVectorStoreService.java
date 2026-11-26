package com.blog.service.rag;

import java.util.List;

public interface RagVectorStoreService {

    void upsertChunks(String sourceType, Long sourceId, List<RagIndexedChunk> chunks);

    void deleteBySource(String sourceType, Long sourceId);

    List<HybridRetrievalChunkHit> search(String query, int limit, double minScore);
}
