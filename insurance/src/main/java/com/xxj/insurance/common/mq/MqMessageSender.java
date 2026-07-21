package com.xxj.insurance.common.mq;

import com.alibaba.fastjson.JSON;
import com.xxj.insurance.domain.po.MqOutbox;
import com.xxj.insurance.mapper.MqOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MQ消息发送器：事务消息表+Confirm回调
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final MqOutboxMapper outboxMapper;

    /** 发送消息（与业务事务同库，事务提交后调用） */
    public void sendAfterCommit(String exchange, String routingKey, String messageType, String businessKey, Object payload) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> body = new HashMap<>();
        body.put("messageId", messageId);
        body.put("messageType", messageType);
        body.put("businessKey", businessKey);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("retryCount", 0);
        body.put("payload", payload);

        String payloadJson = JSON.toJSONString(body);

        // 写 outbox
        MqOutbox outbox = new MqOutbox();
        outbox.setMessageId(messageId);
        outbox.setExchange(exchange);
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(payloadJson);
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        outbox.setCreateTime(LocalDateTime.now());
        outboxMapper.insert(outbox);

        // Confirm 回调
        CorrelationData correlationData = new CorrelationData(messageId);
        correlationData.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            @Override
            public void onSuccess(CorrelationData.Confirm confirm) {
                outbox.setStatus(confirm.isAck() ? 1 : 2);
                if (!confirm.isAck()) { outbox.setErrorMsg(confirm.getReason()); }
                outbox.setUpdateTime(LocalDateTime.now());
                outboxMapper.updateById(outbox);
                log.info("MQ Confirm: messageId={}, ack={}", messageId, confirm.isAck());
            }
            @Override
            public void onFailure(Throwable ex) {
                outbox.setStatus(2); outbox.setErrorMsg(ex.getMessage());
                outbox.setUpdateTime(LocalDateTime.now()); outboxMapper.updateById(outbox);
                log.error("MQ Confirm异常: messageId={}", messageId, ex);
            }
        });

        // 投递
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, body, correlationData);
        } catch (Exception e) {
            outbox.setStatus(2);
            outbox.setErrorMsg(e.getMessage());
            outbox.setUpdateTime(LocalDateTime.now());
            outboxMapper.updateById(outbox);
            log.error("MQ投递异常: messageId={}", messageId, e);
        }
    }

    /** 结算完成 → 门诊队列 */
    public void sendSettleComplete(Long visitId, Object settleData) {
        sendAfterCommit("insurance.settle.direct", "settle.outpatient", "SETTLE_COMPLETE", "settle:" + visitId, settleData);
    }

    /** 拨付完成 → 通知队列 */
    public void sendPayComplete(Long batchId, Object payData) {
        sendAfterCommit("insurance.settle.direct", "audit.notify", "PAY_COMPLETE", "batch:" + batchId, payData);
    }
}
