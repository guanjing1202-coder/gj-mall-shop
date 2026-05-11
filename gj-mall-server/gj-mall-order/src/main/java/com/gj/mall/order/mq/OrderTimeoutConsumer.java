package com.gj.mall.order.mq;

import com.gj.mall.order.config.OrderRabbitConfig;
import com.gj.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = OrderRabbitConfig.TIMEOUT_QUEUE)
    public void onTimeout(Long orderId, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("[order-mq] received timeout-check, orderId={}", orderId);
            if (orderId != null) {
                orderService.timeoutCancel(orderId);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[order-mq] timeout handle failed, orderId={}", orderId, e);
            // 不重试，丢入 dlq 或记录错误（这里直接 ack，不阻塞队列）
            channel.basicAck(deliveryTag, false);
        }
    }
}
