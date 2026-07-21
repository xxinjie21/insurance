package com.xxj.insurance.common.mq;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xxj.insurance.common.config.RabbitMQConfig;
import com.xxj.insurance.service.ISettleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * MQ 消息消费者：手动ACK + 幂等去重
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqConsumers {

    private final MqIdempotentHelper idempotentHelper;
    private final ISettleService settleService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_OUTPATIENT)
    public void onOutpatientSettle(Message message, Channel channel) throws IOException {
        processMessage(message, channel, "门诊结算归档");
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_REMOTE)
    public void onRemoteSettle(Message message, Channel channel) throws IOException {
        processMessage(message, channel, "异地结算归档");
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RECONCILE)
    public void onReconcile(Message message, Channel channel) throws IOException {
        processMessage(message, channel, "批量对账");
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFY)
    public void onNotify(Message message, Channel channel) throws IOException {
        processMessage(message, channel, "审核通知");
    }

    private void processMessage(Message message, Channel channel, String bizName) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.parseObject(new String(message.getBody()), Map.class);
            String messageId = (String) body.get("messageId");

            // 幂等去重：复用 Redisson
            if (!idempotentHelper.tryConsume(messageId)) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("[{}]消费成功: messageId={}", bizName, messageId);
            // 业务消费逻辑：归档/通知等异步任务在此扩展
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[{}]消费异常", bizName, e);
            // 不requeue，由DLX处理
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
