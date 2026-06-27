package com.xxj.insurance.common.constants;

/**
 * 账户相关常量
 *
 * @author xxj
 * @date 2026-04-21
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
}
