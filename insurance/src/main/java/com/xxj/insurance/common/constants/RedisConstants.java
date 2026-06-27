package com.xxj.insurance.common.constants;

// Redis 键名与过期时间定义
public class RedisConstants {

    // ==================== 登录相关 ====================
    // 登录 token 有效期 24 小时（秒）
    public static final Long LOGIN_USER_TTL = 86400L;

    // ==================== 缓存相关 ====================
    // 就诊信息缓存前缀，格式 cache:visit:{visitId}
    public static final String CACHE_VISIT_KEY = "cache:visit:";

    // 就诊缓存过期时间（分钟）
    public static final Long CACHE_VISIT_TTL = 30L;

    // ==================== 管理员选中医院 ====================
    // 管理员选中的医院缓存前缀，格式 admin:selectedHospital:{userId}
    public static final String ADMIN_SELECTED_HOSPITAL_KEY = "admin:selectedHospital:";

    // 管理员选中医院有效期（秒），与登录 token 一致（24小时）
    public static final Long ADMIN_SELECTED_HOSPITAL_TTL = 86400L;
}
