package com.blog.mq;

import com.blog.common.constant.RabbitMqConstants;
import com.blog.mq.event.RagIndexEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagIndexEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishArticleUpsert(Long articleId) {
        publish(RabbitMqConstants.ROUTING_KEY_RAG_ARTICLE_UPSERT, "article", articleId);
    }

    public void publishArticleDelete(Long articleId) {
        publish(RabbitMqConstants.ROUTING_KEY_RAG_ARTICLE_DELETE, "article", articleId);
    }

    public void publishStudyUpsert(Long questionId) {
        publish(RabbitMqConstants.ROUTING_KEY_RAG_STUDY_UPSERT, "study_question", questionId);
    }

    public void publishStudyDelete(Long questionId) {
        publish(RabbitMqConstants.ROUTING_KEY_RAG_STUDY_DELETE, "study_question", questionId);
    }

    private void publish(String routingKey, String sourceType, Long sourceId) {
        if (sourceId == null) {
            return;
        }
        RagIndexEvent event = new RagIndexEvent();
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        try {
            rabbitTemplate.convertAndSend(RabbitMqConstants.EXCHANGE_RAG_INDEX, routingKey, event);
        } catch (Exception e) {
            log.error("发送 RAG 索引事件失败 sourceType={} sourceId={} routingKey={}", sourceType, sourceId, routingKey, e);
        }
    }
}
