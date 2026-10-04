package com.uit.petrescueapi.infrastructure.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event-driven social media features.
 * 
 * Architecture:
 * - Single topic exchange (social.exchange)
 * - 5 queues for different event types
 * - JSON message converter for domain events
 * - Dead Letter Queue for failed messages
 */
@Configuration
@Slf4j
@Getter
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.social}")
    private String socialExchange;

    @Value("${app.rabbitmq.queue.post-liked}")
    private String postLikedQueue;

    @Value("${app.rabbitmq.queue.post-unliked}")
    private String postUnlikedQueue;

    @Value("${app.rabbitmq.queue.comment-created}")
    private String commentCreatedQueue;

    @Value("${app.rabbitmq.queue.comment-liked}")
    private String commentLikedQueue;

    @Value("${app.rabbitmq.queue.comment-unliked}")
    private String commentUnlikedQueue;

    @Value("${app.rabbitmq.routing-key.post-liked}")
    private String postLikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.post-unliked}")
    private String postUnlikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-created}")
    private String commentCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-liked}")
    private String commentLikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-unliked}")
    private String commentUnlikedRoutingKey;

    @Value("${app.rabbit.listener.concurrency:1}")
    private int concurrentConsumers;

    @Value("${app.rabbit.listener.max-concurrency:1}")
    private int maxConcurrentConsumers;

    // ===== Exchange =====

    @Bean
    public TopicExchange socialExchange() {
        return new TopicExchange(socialExchange, true, false);
    }

    // ===== Dead Letter Exchange & Queue =====

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("social.dlx", true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("social.dlq").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlq");
    }

    // ===== Queues with DLQ support =====

    @Bean
    public Queue postLikedQueue() {
        return QueueBuilder.durable(postLikedQueue)
                .withArgument("x-dead-letter-exchange", "social.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue postUnlikedQueue() {
        return QueueBuilder.durable(postUnlikedQueue)
                .withArgument("x-dead-letter-exchange", "social.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue commentCreatedQueue() {
        return QueueBuilder.durable(commentCreatedQueue)
                .withArgument("x-dead-letter-exchange", "social.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue commentLikedQueue() {
        return QueueBuilder.durable(commentLikedQueue)
                .withArgument("x-dead-letter-exchange", "social.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue commentUnlikedQueue() {
        return QueueBuilder.durable(commentUnlikedQueue)
                .withArgument("x-dead-letter-exchange", "social.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    // ===== Bindings =====

    @Bean
    public Binding postLikedBinding() {
        return BindingBuilder.bind(postLikedQueue())
                .to(socialExchange())
                .with(postLikedRoutingKey);
    }

    @Bean
    public Binding postUnlikedBinding() {
        return BindingBuilder.bind(postUnlikedQueue())
                .to(socialExchange())
                .with(postUnlikedRoutingKey);
    }

    @Bean
    public Binding commentCreatedBinding() {
        return BindingBuilder.bind(commentCreatedQueue())
                .to(socialExchange())
                .with(commentCreatedRoutingKey);
    }

    @Bean
    public Binding commentLikedBinding() {
        return BindingBuilder.bind(commentLikedQueue())
                .to(socialExchange())
                .with(commentLikedRoutingKey);
    }

    @Bean
    public Binding commentUnlikedBinding() {
        return BindingBuilder.bind(commentUnlikedQueue())
                .to(socialExchange())
                .with(commentUnlikedRoutingKey);
    }

    // ===== Message Converter =====

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        return factory;
    }
}
