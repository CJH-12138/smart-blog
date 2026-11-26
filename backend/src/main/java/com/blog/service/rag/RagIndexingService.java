package com.blog.service.rag;

import java.util.Map;

public interface RagIndexingService {

    void upsertArticle(Long articleId);

    void deleteArticle(Long articleId);

    void upsertStudyQuestion(Long questionId);

    void deleteStudyQuestion(Long questionId);

    Map<String, Object> rebuild(String scope);

    Map<String, Object> getStatus();
}
