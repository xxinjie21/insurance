package com.xxj.insurance.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 结算表
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("settle")

public class Settle implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    private Long visitId;

    
    private Long hospitalId;

    
    private BigDecimal total;

    
    private BigDecimal reimburse;

    
    private BigDecimal selfPay;

    /**
     * 统筹支付金额
     */
    private BigDecimal poolingPay;

    /**
     * 个人账户支付金额
     */
    private BigDecimal accountPay;

    /**
     * 个人现金支付金额
     */
    private BigDecimal cashPay;

    private Integer status;

    
    private LocalDateTime createTime;


}
