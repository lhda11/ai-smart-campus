package com.campus.core.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String QUEUE_VECTOR_SYNC = "campus.vector.sync";
    public static final String QUEUE_VECTOR_DELETE = "campus.vector.delete";

    @Bean
    public Queue vectorSyncQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_SYNC)
                .ttl(60000)
                .deadLetterExchange("")
                .deadLetterRoutingKey("campus.vector.sync.dlq")
                .build();
    }

    @Bean
    public Queue vectorSyncDlq() {
        return QueueBuilder.durable("campus.vector.sync.dlq").build();
    }

    @Bean
    public Queue vectorDeleteQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_DELETE).build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
