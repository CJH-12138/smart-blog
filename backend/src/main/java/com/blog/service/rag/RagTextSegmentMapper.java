package com.blog.service.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RagTextSegmentMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private RagTextSegmentMapper() {
    }

    public static TextSegment toSegment(RagIndexedChunk chunk) {
        Metadata metadata = new Metadata()
                .put("sourceType", safe(chunk.getSourceType()))
                .put("sourceId", chunk.getSourceId() == null ? 0L : chunk.getSourceId())
                .put("chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex())
                .put("title", safe(chunk.getTitle()))
                .put("summary", safe(chunk.getSummary()))
                .put("categoryId", chunk.getCategoryId() == null ? 0L : chunk.getCategoryId())
                .put("categoryName", safe(chunk.getCategoryName()))
                .put("tagIds", joinLongs(chunk.getTagIds()))
                .put("tagNames", joinStrings(chunk.getTagNames()))
                .put("authorId", chunk.getAuthorId() == null ? 0L : chunk.getAuthorId())
                .put("status", chunk.getStatus() == null ? 0 : chunk.getStatus())
                .put("keywords", safe(chunk.getKeywords()))
                .put("difficulty", chunk.getDifficulty() == null ? 0 : chunk.getDifficulty())
                .put("publishTime", formatDateTime(chunk.getPublishTime()))
                .put("updatedTime", formatDateTime(chunk.getUpdatedTime()));
        return TextSegment.from(chunk.getContent(), metadata);
    }

    public static HybridRetrievalChunkHit toHit(TextSegment segment, Double vectorScore) {
        Metadata metadata = segment.metadata();
        Map<String, Object> values = metadata.toMap();
        return HybridRetrievalChunkHit.builder()
                .sourceType(asString(values.get("sourceType")))
                .sourceId(asLong(values.get("sourceId")))
                .chunkIndex(asInteger(values.get("chunkIndex")))
                .title(asString(values.get("title")))
                .content(segment.text())
                .summary(asString(values.get("summary")))
                .categoryId(asLong(values.get("categoryId")))
                .categoryName(asString(values.get("categoryName")))
                .keywords(asString(values.get("keywords")))
                .difficulty(asInteger(values.get("difficulty")))
                .publishTime(parseDateTime(asString(values.get("publishTime"))))
                .vectorScore(vectorScore == null ? 0D : vectorScore)
                .build();
    }

    public static String joinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static String joinStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().filter(StringUtils::hasText).collect(Collectors.joining(","));
    }

    public static List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return List.of(value.split(","));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return StringUtils.hasText(asString(value)) ? Long.parseLong(asString(value)) : null;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return StringUtils.hasText(asString(value)) ? Integer.parseInt(asString(value)) : null;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
