package com.gj.mall.order.mq;

import com.gj.mall.order.config.OrderRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 发送订单延迟超时检查消息（消息体 = 订单 ID） */
    public void send(Long orderId) {
        if (orderId == null) return;
        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.DELAY_EXCHANGE,
                OrderRabbitConfig.DELAY_ROUTE_KEY,
                orderId);
        log.info("[order-mq] sent timeout-check, orderId={}", orderId);
    }
}
