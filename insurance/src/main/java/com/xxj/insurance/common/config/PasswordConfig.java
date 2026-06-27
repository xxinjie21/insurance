package com.xxj.insurance.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * 使用 BCrypt 替代 MD5，每个密码独立加盐，防彩虹表攻击
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost factor = 10，兼顾安全性与性能
        return new BCryptPasswordEncoder(10);
    }
}
