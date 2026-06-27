package com.xxj.insurance.common.constants;

import java.math.BigDecimal;

// 医保报销相关常量
public class ReimburseConstants {

    // 甲类 100% 报销
    public static final BigDecimal CATEGORY_A_RATE = new BigDecimal("1.0");

    // 乙类 80% 报销
    public static final BigDecimal CATEGORY_B_RATE = new BigDecimal("0.8");

    // 自费项目 0% 报销
    public static final BigDecimal CATEGORY_C_RATE = BigDecimal.ZERO;

    // 结算状态：0-待申报
    public static final Integer SETTLE_STATUS_UNDECLARED = 0;

    // 结算状态：1-已申报
    public static final Integer SETTLE_STATUS_DECLARED = 1;

    // 结算状态：2-患者已支付自付部分
    public static final Integer SETTLE_STATUS_SELF_PAID = 2;

    // 结算状态：3-已拨付
    public static final Integer SETTLE_STATUS_PAID = 3;

    // 就诊状态：0-就诊中，待结算
    public static final Integer VISIT_STATUS_PENDING = 0;

    // 就诊状态：1-已结算
    public static final Integer VISIT_STATUS_SETTLED = 1;

    // 批次状态：0-待申报（新建）
    public static final Integer BATCH_STATUS_PENDING = 0;

    // 批次状态：1-已申报
    public static final Integer BATCH_STATUS_DECLARED = 1;

    // 批次状态：2-已完成（审核通过已拨付）
    public static final Integer BATCH_STATUS_COMPLETED = 2;

    // 批次状态：3-拨付被拒
    public static final Integer BATCH_STATUS_PAY_REJECTED = 3;

    // 审核通过
    public static final Integer AUDIT_PASS = 0;

    // 已支付
    public static final Integer PAY_STATUS_PAID = 1;

    // 拒绝拨付
    public static final Integer PAY_STATUS_REJECTED = 2;
}
