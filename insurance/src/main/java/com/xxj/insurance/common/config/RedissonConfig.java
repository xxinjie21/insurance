package com.xxj.insurance.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 * 
 * @author xxj
 * @date 2026-04-21
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * 创建 Redisson 客户端
     * 
     * @return RedissonClient
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 构建 Redis 地址
        String redisAddress = String.format("redis://%s:%d", redisHost, redisPort);
        
        // 使用单节点配置（如果是集群或哨兵模式，需要改用相应配置）
        var serverConfig = config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000);
        
        // 如果有密码，设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
