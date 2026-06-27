package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录 VO
 */
@Data
public class RechargeRecordVO {
    
    /**
     * 充值记录 ID
     */
    private Long id;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 充值订单号
     */
    private String orderNo;
    
    /**
     * 充值金额
     */
    private BigDecimal amount;
    
    /**
     * 充值类型：1-微信 2-支付宝 3-银行卡 4-现金
     */
    private Integer type;
    
    /**
     * 状态：0-待支付 1-支付成功 2-支付失败 3-已退款
     */
    private Integer status;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
