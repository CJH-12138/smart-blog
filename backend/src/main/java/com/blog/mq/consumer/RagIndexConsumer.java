package com.blog.mq.consumer;

import com.blog.common.constant.RabbitMqConstants;
import com.blog.mq.event.RagIndexEvent;
import com.blog.service.rag.RagIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagIndexConsumer {

    private final RagIndexingService ragIndexingService;

    @RabbitListener(queues = RabbitMqConstants.QUEUE_RAG_ARTICLE_UPSERT)
    public void consumeArticleUpsert(RagIndexEvent event) {
        if (event == null) {
            return;
        }
        ragIndexingService.upsertArticle(event.getSourceId());
    }

    @RabbitListener(queues = RabbitMqConstants.QUEUE_RAG_ARTICLE_DELETE)
    public void consumeArticleDelete(RagIndexEvent event) {
        if (event == null) {
            return;
        }
        ragIndexingService.deleteArticle(event.getSourceId());
    }

    @RabbitListener(queues = RabbitMqConstants.QUEUE_RAG_STUDY_UPSERT)
    public void consumeStudyUpsert(RagIndexEvent event) {
        if (event == null) {
            return;
        }
        ragIndexingService.upsertStudyQuestion(event.getSourceId());
    }

    @RabbitListener(queues = RabbitMqConstants.QUEUE_RAG_STUDY_DELETE)
    public void consumeStudyDelete(RagIndexEvent event) {
        if (event == null) {
            return;
        }
        ragIndexingService.deleteStudyQuestion(event.getSourceId());
    }
}
