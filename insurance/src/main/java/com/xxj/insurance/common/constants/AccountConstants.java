package com.xxj.insurance.common.constants;

import java.math.BigDecimal;

/**
 * 账户相关常量
 *
 * @author xxj
 * @since 2026-04-21
 */
public class AccountConstants {

    /**
     * 账户状态：0-冻结
     */
    public static final Integer ACCOUNT_STATUS_FROZEN = 0;

    /**
     * 账户状态：1-正常
     */
    public static final Integer ACCOUNT_STATUS_NORMAL = 1;

    /**
     * 充值状态：1-支付成功
     */
    public static final Integer RECHARGE_STATUS_SUCCESS = 1;

    /**
     * 消费类型：1-就诊支付
     */
    public static final Integer CONSUMPTION_TYPE_VISIT_PAY = 1;

    /**
     * 消费状态：1-成功
     */
    public static final Integer CONSUMPTION_STATUS_SUCCESS = 1;

    /**
     * 充值订单号前缀
     */
    public static final String RECHARGE_ORDER_PREFIX = "RC";

    /**
     * 消费订单号前缀
     */
    public static final String CONSUMPTION_ORDER_PREFIX = "CP";

    /**
     * 职工医保个人账户初始余额（模拟每月划入）
     */
    public static final BigDecimal PERSONAL_ACCOUNT_INIT_BALANCE = new BigDecimal("5000.00");

    /**
     * 流水类型：1-个人账户划入(系统充值)
     */
    public static final Integer FLOW_TYPE_ACCOUNT_IN = 1;

    /**
     * 流水类型：2-就诊支付扣款
     */
    public static final Integer FLOW_TYPE_VISIT_PAY = 2;

    /**
     * 流水类型：3-退款
     */
    public static final Integer FLOW_TYPE_REFUND = 3;
}
