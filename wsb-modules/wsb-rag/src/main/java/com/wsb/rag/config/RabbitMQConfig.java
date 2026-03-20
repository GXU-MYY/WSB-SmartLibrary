package com.wsb.rag.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rag.exchange}")
    private String exchange;

    @Value("${rag.queue.summary}")
    private String summaryQueue;

    @Value("${rag.queue.embedding}")
    private String embeddingQueue;

    @Value("${rag.routing.summary}")
    private String summaryRoutingKey;

    @Value("${rag.routing.embedding}")
    private String embeddingRoutingKey;

    @Bean
    public DirectExchange ragExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable(summaryQueue).build();
    }

    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(embeddingQueue).build();
    }

    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue()).to(ragExchange()).with(summaryRoutingKey);
    }

    @Bean
    public Binding embeddingBinding() {
        return BindingBuilder.bind(embeddingQueue()).to(ragExchange()).with(embeddingRoutingKey);
    }
}