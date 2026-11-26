package com.blog.config;

import com.blog.common.constant.RabbitMqConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange userExpExchange() {
        return new TopicExchange(RabbitMqConstants.EXCHANGE_USER_EXP, true, false);
    }

    @Bean
    public TopicExchange ragIndexExchange() {
        return new TopicExchange(RabbitMqConstants.EXCHANGE_RAG_INDEX, true, false);
    }

    @Bean
    public Queue expGrantQueue() {
        return new Queue(RabbitMqConstants.QUEUE_EXP_GRANT, true);
    }

    @Bean
    public Queue expNotifyQueue() {
        return new Queue(RabbitMqConstants.QUEUE_EXP_NOTIFY, true);
    }

    @Bean
    public Queue expRankQueue() {
        return new Queue(RabbitMqConstants.QUEUE_EXP_RANK, true);
    }

    @Bean
    public Queue ragArticleUpsertQueue() {
        return new Queue(RabbitMqConstants.QUEUE_RAG_ARTICLE_UPSERT, true);
    }

    @Bean
    public Queue ragArticleDeleteQueue() {
        return new Queue(RabbitMqConstants.QUEUE_RAG_ARTICLE_DELETE, true);
    }

    @Bean
    public Queue ragStudyUpsertQueue() {
        return new Queue(RabbitMqConstants.QUEUE_RAG_STUDY_UPSERT, true);
    }

    @Bean
    public Queue ragStudyDeleteQueue() {
        return new Queue(RabbitMqConstants.QUEUE_RAG_STUDY_DELETE, true);
    }

    @Bean
    public Binding expGrantBinding(@Qualifier("expGrantQueue") Queue expGrantQueue, TopicExchange userExpExchange) {
        return BindingBuilder.bind(expGrantQueue).to(userExpExchange).with(RabbitMqConstants.ROUTING_KEY_EXP_GRANT);
    }

    @Bean
    public Binding expNotifyBinding(@Qualifier("expNotifyQueue") Queue expNotifyQueue, TopicExchange userExpExchange) {
        return BindingBuilder.bind(expNotifyQueue).to(userExpExchange).with(RabbitMqConstants.ROUTING_KEY_EXP_CHANGED);
    }

    @Bean
    public Binding expRankBinding(@Qualifier("expRankQueue") Queue expRankQueue, TopicExchange userExpExchange) {
        return BindingBuilder.bind(expRankQueue).to(userExpExchange).with(RabbitMqConstants.ROUTING_KEY_EXP_CHANGED);
    }

    @Bean
    public Binding ragArticleUpsertBinding(@Qualifier("ragArticleUpsertQueue") Queue queue, TopicExchange ragIndexExchange) {
        return BindingBuilder.bind(queue).to(ragIndexExchange).with(RabbitMqConstants.ROUTING_KEY_RAG_ARTICLE_UPSERT);
    }

    @Bean
    public Binding ragArticleDeleteBinding(@Qualifier("ragArticleDeleteQueue") Queue queue, TopicExchange ragIndexExchange) {
        return BindingBuilder.bind(queue).to(ragIndexExchange).with(RabbitMqConstants.ROUTING_KEY_RAG_ARTICLE_DELETE);
    }

    @Bean
    public Binding ragStudyUpsertBinding(@Qualifier("ragStudyUpsertQueue") Queue queue, TopicExchange ragIndexExchange) {
        return BindingBuilder.bind(queue).to(ragIndexExchange).with(RabbitMqConstants.ROUTING_KEY_RAG_STUDY_UPSERT);
    }

    @Bean
    public Binding ragStudyDeleteBinding(@Qualifier("ragStudyDeleteQueue") Queue queue, TopicExchange ragIndexExchange) {
        return BindingBuilder.bind(queue).to(ragIndexExchange).with(RabbitMqConstants.ROUTING_KEY_RAG_STUDY_DELETE);
    }
}
