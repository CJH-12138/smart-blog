package com.blog.service.rag;

import com.blog.dto.response.SearchAllVO;

public interface HybridRetrievalService {

    SearchAllVO search(String keyword, int pageSize);
}
