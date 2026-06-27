package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费记录 VO
 */
@Data
public class ConsumptionRecordVO {
    
    /**
     * 消费记录 ID
     */
    private Long id;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 就诊 ID
     */
    private Long visitId;
    
    /**
     * 结算 ID
     */
    private Long settleId;
    
    /**
     * 消费订单号
     */
    private String orderNo;
    
    /**
     * 消费金额
     */
    private BigDecimal amount;
    
    /**
     * 消费类型：1-就诊支付 2-退款 3-调整
     */
    private Integer type;
    
    /**
     * 状态：0-取消 1-成功
     */
    private Integer status;
    
    /**
     * 消费前余额
     */
    private BigDecimal balanceBefore;
    
    /**
     * 消费后余额
     */
    private BigDecimal balanceAfter;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
