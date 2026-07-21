package com.xxj.insurance.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MQ消费幂等：复用Redisson体系，messageId做Redis幂等Key
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqIdempotentHelper {

    private static final String IDEMPOTENT_PREFIX = "idempotent:mq:";
    private static final long TTL_HOURS = 24;

    private final RedissonClient redissonClient;

    /** 检查消息是否已消费，未消费则标记 */
    public boolean tryConsume(String messageId) {
        String key = IDEMPOTENT_PREFIX + messageId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean exists = bucket.isExists();
        if (exists) {
            log.info("MQ消息重复，跳过消费: messageId={}", messageId);
            return false;
        }
        bucket.set("1", TTL_HOURS, TimeUnit.HOURS);
        return true;
    }
}
