package com.blog.common.constant;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String EXCHANGE_USER_EXP = "blog.user.exp.exchange";

    public static final String ROUTING_KEY_EXP_GRANT = "exp.grant";
    public static final String ROUTING_KEY_EXP_CHANGED = "exp.changed";

    public static final String QUEUE_EXP_GRANT = "blog.user.exp.grant.queue";
    public static final String QUEUE_EXP_NOTIFY = "blog.user.exp.notify.queue";
    public static final String QUEUE_EXP_RANK = "blog.user.exp.rank.queue";

    public static final String EXCHANGE_RAG_INDEX = "blog.rag.index.exchange";

    public static final String ROUTING_KEY_RAG_ARTICLE_UPSERT = "rag.article.upsert";
    public static final String ROUTING_KEY_RAG_ARTICLE_DELETE = "rag.article.delete";
    public static final String ROUTING_KEY_RAG_STUDY_UPSERT = "rag.study.upsert";
    public static final String ROUTING_KEY_RAG_STUDY_DELETE = "rag.study.delete";

    public static final String QUEUE_RAG_ARTICLE_UPSERT = "blog.rag.article.upsert.queue";
    public static final String QUEUE_RAG_ARTICLE_DELETE = "blog.rag.article.delete.queue";
    public static final String QUEUE_RAG_STUDY_UPSERT = "blog.rag.study.upsert.queue";
    public static final String QUEUE_RAG_STUDY_DELETE = "blog.rag.study.delete.queue";
}
