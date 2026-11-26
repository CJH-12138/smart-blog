package com.blog.controller.admin;

import com.blog.common.result.Result;
import com.blog.service.rag.RagIndexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理端-AI RAG")
@RestController
@RequestMapping("/api/admin/ai/rag")
@RequiredArgsConstructor
public class AdminAiRagController {

    private final RagIndexingService ragIndexingService;

    @Operation(summary = "获取 RAG 状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(ragIndexingService.getStatus());
    }

    @Operation(summary = "重建 RAG 索引")
    @PostMapping("/reindex")
    public Result<Map<String, Object>> rebuild(@RequestParam(defaultValue = "all") String scope) {
        return Result.success(ragIndexingService.rebuild(scope));
    }

    @Operation(summary = "重建单条 RAG 索引")
    @PostMapping("/reindex/{sourceType}/{sourceId}")
    public Result<?> rebuildOne(@PathVariable String sourceType, @PathVariable Long sourceId) {
        if ("article".equalsIgnoreCase(sourceType)) {
            ragIndexingService.upsertArticle(sourceId);
        } else if ("study".equalsIgnoreCase(sourceType) || "study_question".equalsIgnoreCase(sourceType)) {
            ragIndexingService.upsertStudyQuestion(sourceId);
        }
        return Result.success();
    }

    @Operation(summary = "删除单条 RAG 索引")
    @DeleteMapping("/index/{sourceType}/{sourceId}")
    public Result<?> deleteOne(@PathVariable String sourceType, @PathVariable Long sourceId) {
        if ("article".equalsIgnoreCase(sourceType)) {
            ragIndexingService.deleteArticle(sourceId);
        } else if ("study".equalsIgnoreCase(sourceType) || "study_question".equalsIgnoreCase(sourceType)) {
            ragIndexingService.deleteStudyQuestion(sourceId);
        }
        return Result.success();
    }
}
