package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户信息 VO
 */
@Data
public class UserAccountVO {
    
    /**
     * 账户 ID
     */
    private Long id;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 用户姓名
     */
    private String userName;
    
    /**
     * 账户余额
     */
    private BigDecimal balance;
    
    /**
     * 累计充值金额
     */
    private BigDecimal totalRecharge;
    
    /**
     * 累计消费金额
     */
    private BigDecimal totalConsumption;
    
    /**
     * 状态：0-冻结 1-正常
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
