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

    
    private Integer status;

    
    private LocalDateTime createTime;


}
