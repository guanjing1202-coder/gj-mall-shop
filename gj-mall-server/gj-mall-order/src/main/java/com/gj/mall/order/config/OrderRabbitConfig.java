package com.gj.mall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单延迟队列：用「TTL + DLX」方案，无需 rabbitmq-delayed-message-exchange 插件
 *
 *   producer -> order.delay.exchange -> order.delay.queue (TTL)
 *                                          ↓ expired
 *                                      order.dlx.exchange -> order.timeout.queue -> consumer
 */
@Configuration
public class OrderRabbitConfig {

    public static final String DELAY_EXCHANGE  = "order.delay.exchange";
    public static final String DELAY_QUEUE     = "order.delay.queue";
    public static final String DELAY_ROUTE_KEY = "order.delay";

    public static final String DLX_EXCHANGE     = "order.dlx.exchange";
    public static final String TIMEOUT_QUEUE    = "order.timeout.queue";
    public static final String TIMEOUT_ROUTE_KEY = "order.timeout";

    /** 订单超时（分钟），默认 30 */
    @Value("${mall.order.timeout-minutes:30}")
    private int timeoutMinutes;

    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder.directExchange(DELAY_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange orderDlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", timeoutMinutes * 60_000);
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", TIMEOUT_ROUTE_KEY);
        return QueueBuilder.durable(DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding bindDelay() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(DELAY_ROUTE_KEY);
    }

    @Bean
    public Binding bindTimeout() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderDlxExchange()).with(TIMEOUT_ROUTE_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 覆盖 Spring Boot 自动装配的 RabbitTemplate，统一使用 JSON 序列化 */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }
}
