package com.wsb.rag.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
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

    @Value("${rag.mq.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${rag.mq.retry.initial-interval-ms:1000}")
    private long retryInitialIntervalMs;

    @Value("${rag.mq.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${rag.mq.retry.max-interval-ms:10000}")
    private long retryMaxIntervalMs;

    @Value("${rag.mq.prefetch-count:4}")
    private int prefetchCount;

    @Bean
    public DirectExchange ragExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public DirectExchange ragDeadLetterExchange() {
        return new DirectExchange(deadLetterExchange());
    }

    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable(summaryQueue)
                .deadLetterExchange(deadLetterExchange())
                .deadLetterRoutingKey(summaryDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(embeddingQueue)
                .deadLetterExchange(deadLetterExchange())
                .deadLetterRoutingKey(embeddingDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue summaryDeadLetterQueue() {
        return QueueBuilder.durable(summaryQueue + ".dlq").build();
    }

    @Bean
    public Queue embeddingDeadLetterQueue() {
        return QueueBuilder.durable(embeddingQueue + ".dlq").build();
    }

    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue()).to(ragExchange()).with(summaryRoutingKey);
    }

    @Bean
    public Binding embeddingBinding() {
        return BindingBuilder.bind(embeddingQueue()).to(ragExchange()).with(embeddingRoutingKey);
    }

    @Bean
    public Binding summaryDeadLetterBinding() {
        return BindingBuilder.bind(summaryDeadLetterQueue())
                .to(ragDeadLetterExchange())
                .with(summaryDeadLetterRoutingKey());
    }

    @Bean
    public Binding embeddingDeadLetterBinding() {
        return BindingBuilder.bind(embeddingDeadLetterQueue())
                .to(ragDeadLetterExchange())
                .with(embeddingDeadLetterRoutingKey());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory ragRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(Math.max(prefetchCount, 1));
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(Math.max(retryMaxAttempts, 1))
                .backOffOptions(Math.max(retryInitialIntervalMs, 1), Math.max(retryMultiplier, 1.0),
                        Math.max(retryMaxIntervalMs, retryInitialIntervalMs))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    private String deadLetterExchange() {
        return exchange + ".dlx";
    }

    private String summaryDeadLetterRoutingKey() {
        return summaryQueue + ".dlq";
    }

    private String embeddingDeadLetterRoutingKey() {
        return embeddingQueue + ".dlq";
    }
}
