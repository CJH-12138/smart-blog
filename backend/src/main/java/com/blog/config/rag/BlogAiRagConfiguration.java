package com.blog.config.rag;

import com.blog.config.ElasticsearchProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties(BlogAiRagProperties.class)
public class BlogAiRagConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "blog.ai.rag", name = "enabled", havingValue = "true")
    public RestClient ragElasticsearchRestClient(ElasticsearchProperties elasticsearchProperties) {
        String endpoint = elasticsearchProperties.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("RAG 已启用，但未配置 Elasticsearch endpoint");
        }
        RestClientBuilder builder = RestClient.builder(HttpHost.create(endpoint));
        List<Header> headers = new ArrayList<>();
        if (StringUtils.hasText(elasticsearchProperties.getUsername())) {
            String credential = elasticsearchProperties.getUsername() + ":" +
                    (elasticsearchProperties.getPassword() == null ? "" : elasticsearchProperties.getPassword());
            String auth = Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
            headers.add(new BasicHeader("Authorization", "Basic " + auth));
        }
        if (!headers.isEmpty()) {
            builder.setDefaultHeaders(headers.toArray(new Header[0]));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.ai.rag", name = "enabled", havingValue = "true")
    public EmbeddingModel ragEmbeddingModel(BlogAiRagProperties properties) {
        BlogAiRagProperties.Embedding embedding = properties.getEmbedding();
        if (!StringUtils.hasText(embedding.getApiKey()) || !StringUtils.hasText(embedding.getModel())) {
            throw new IllegalStateException("RAG 已启用，但未配置 embedding apiKey/model");
        }
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(embedding.getApiKey())
                .modelName(embedding.getModel())
                .maxRetries(embedding.getMaxRetries())
                .maxSegmentsPerBatch(embedding.getMaxSegmentsPerBatch());
        if (StringUtils.hasText(embedding.getBaseUrl())) {
            builder.baseUrl(embedding.getBaseUrl());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.ai.rag", name = "enabled", havingValue = "true")
    public EmbeddingStore<TextSegment> ragEmbeddingStore(RestClient ragElasticsearchRestClient,
                                                         BlogAiRagProperties properties) {
        return ElasticsearchEmbeddingStore.builder()
                .restClient(ragElasticsearchRestClient)
                .indexName(properties.getIndexName())
                .configuration(ElasticsearchConfigurationKnn.builder().build())
                .build();
    }
}
