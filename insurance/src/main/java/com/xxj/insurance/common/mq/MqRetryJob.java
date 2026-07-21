package com.xxj.insurance.common.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.domain.po.MqOutbox;
import com.xxj.insurance.mapper.MqOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 兜底补偿Job：每5分钟扫描失败/未发送的outbox，补推
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqRetryJob {

    private final MqOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_RETRY = 5;

    @Scheduled(fixedDelay = 300000) // 5分钟
    public void retryFailedMessages() {
        List<MqOutbox> failed = outboxMapper.selectList(new LambdaQueryWrapper<MqOutbox>()
                .in(MqOutbox::getStatus, 0, 2)
                .lt(MqOutbox::getRetryCount, MAX_RETRY));
        if (failed.isEmpty()) return;

        log.info("MQ补推任务: 待处理{}条", failed.size());
        for (MqOutbox outbox : failed) {
            try {
                Map<String, Object> body = JSON.parseObject(outbox.getPayload(), Map.class);
                body.put("retryCount", outbox.getRetryCount() + 1);
                String updatedPayload = JSON.toJSONString(body);
                outbox.setPayload(updatedPayload);

                rabbitTemplate.convertAndSend(outbox.getExchange(), outbox.getRoutingKey(),
                        body, new org.springframework.amqp.rabbit.connection.CorrelationData(outbox.getMessageId()));
                outbox.setStatus(1);
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                outbox.setErrorMsg(null);
                outbox.setUpdateTime(LocalDateTime.now());
                outboxMapper.updateById(outbox);
                log.info("MQ补推成功: messageId={}", outbox.getMessageId());
            } catch (Exception e) {
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                outbox.setErrorMsg(e.getMessage());
                outbox.setUpdateTime(LocalDateTime.now());
                outboxMapper.updateById(outbox);
                log.error("MQ补推失败: messageId={}", outbox.getMessageId(), e);
            }
        }
    }
}
