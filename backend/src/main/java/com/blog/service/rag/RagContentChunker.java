package com.blog.service.rag;

import cn.hutool.http.HtmlUtil;
import com.blog.config.rag.BlogAiRagProperties;
import com.blog.entity.Article;
import com.blog.entity.StudyQuestion;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RagContentChunker {

    private final BlogAiRagProperties properties;

    public RagContentChunker(BlogAiRagProperties properties) {
        this.properties = properties;
    }

    public List<RagIndexedChunk> chunkArticle(Article article, List<Long> tagIds, List<String> tagNames) {
        if (article == null || article.getId() == null) {
            return Collections.emptyList();
        }
        String normalized = cleanText(StringUtils.hasText(article.getContentHtml()) ? article.getContentHtml() : article.getContent());
        if (!StringUtils.hasText(normalized)) {
            normalized = cleanText(article.getSummary());
        }
        List<String> pieces = splitByLength(normalized,
                safePositive(properties.getChunk().getArticleMaxLength(), 800),
                Math.max(0, safePositive(properties.getChunk().getArticleOverlap(), 120)));
        if (pieces.isEmpty()) {
            pieces = List.of(defaultText(article.getSummary(), article.getTitle(), "无内容"));
        }

        List<RagIndexedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            chunks.add(RagIndexedChunk.builder()
                    .id(buildChunkId("article", article.getId(), i))
                    .sourceType("article")
                    .sourceId(article.getId())
                    .chunkIndex(i)
                    .title(defaultText(article.getTitle(), "未命名文章"))
                    .content(pieces.get(i))
                    .summary(defaultText(article.getSummary(), shorten(pieces.get(i), 120), "无"))
                    .categoryId(article.getCategoryId())
                    .categoryName(defaultText(article.getCategoryName(), "未分类"))
                    .tagIds(tagIds == null ? Collections.emptyList() : tagIds)
                    .tagNames(tagNames == null ? Collections.emptyList() : tagNames)
                    .authorId(article.getUserId())
                    .status(article.getStatus())
                    .publishTime(article.getPublishTime())
                    .updatedTime(article.getUpdateTime())
                    .build());
        }
        return chunks;
    }

    public List<RagIndexedChunk> chunkStudyQuestion(StudyQuestion question, String categoryName) {
        if (question == null || question.getId() == null) {
            return Collections.emptyList();
        }
        String merged = String.join("\n",
                defaultText(question.getTitle(), ""),
                defaultText(question.getQuestionStem(), ""),
                defaultText(question.getAnswerSummary(), ""),
                defaultText(question.getStandardAnswer(), "")
        ).trim();
        List<String> pieces = splitByLength(cleanText(merged),
                safePositive(properties.getChunk().getQuestionMaxLength(), 600),
                0);
        if (pieces.isEmpty()) {
            pieces = List.of(defaultText(question.getTitle(), "无内容"));
        }

        List<RagIndexedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            chunks.add(RagIndexedChunk.builder()
                    .id(buildChunkId("study_question", question.getId(), i))
                    .sourceType("study_question")
                    .sourceId(question.getId())
                    .chunkIndex(i)
                    .title(defaultText(question.getTitle(), "未命名题目"))
                    .content(pieces.get(i))
                    .summary(defaultText(question.getAnswerSummary(), shorten(pieces.get(i), 120), "无"))
                    .categoryId(question.getCategoryId())
                    .categoryName(defaultText(categoryName, "未分类"))
                    .status(question.getStatus())
                    .keywords(defaultText(question.getKeywords(), ""))
                    .difficulty(question.getDifficulty())
                    .publishTime(question.getPublishTime())
                    .updatedTime(question.getUpdateTime())
                    .build());
        }
        return chunks;
    }

    static String buildChunkId(String sourceType, Long sourceId, int chunkIndex) {
        return sourceType + "-" + sourceId + "-" + chunkIndex;
    }

    private List<String> splitByLength(String text, int maxLength, int overlap) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        List<String> paragraphs = splitParagraphs(text);
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            if (paragraph.length() > maxLength) {
                if (current.length() > 0) {
                    segments.add(current.toString().trim());
                    current = new StringBuilder();
                }
                segments.addAll(splitLongText(paragraph, maxLength, overlap));
                continue;
            }
            if (current.length() == 0) {
                current.append(paragraph);
                continue;
            }
            if (current.length() + 1 + paragraph.length() <= maxLength) {
                current.append('\n').append(paragraph);
                continue;
            }
            segments.add(current.toString().trim());
            current = new StringBuilder(paragraph);
        }
        if (current.length() > 0) {
            segments.add(current.toString().trim());
        }
        return segments.stream().filter(StringUtils::hasText).collect(Collectors.toList());
    }

    private List<String> splitParagraphs(String text) {
        String normalized = text.replace("\r", "\n")
                .replaceAll("\n{2,}", "\n")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return Collections.emptyList();
        }
        return List.of(normalized.split("\n"));
    }

    private List<String> splitLongText(String text, int maxLength, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            result.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return result;
    }

    private String cleanText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String value = HtmlUtil.cleanHtmlTag(text);
        value = value.replaceAll("\\[(.*?)]\\((.*?)\\)", "$1")
                .replaceAll("`{1,3}", " ")
                .replaceAll("#{1,6}", " ")
                .replaceAll("\\*|>|\\-|\\|", " ")
                .replaceAll("[ \t\\x0B\f\r]+", " ")
                .replaceAll(" *\n+ *", "\n")
                .trim();
        return value;
    }

    private int safePositive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String shorten(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private String defaultText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
