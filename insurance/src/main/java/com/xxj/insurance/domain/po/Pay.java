package com.xxj.insurance.domain.po;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * 基金拨付表
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pay")

public class Pay implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    private Long batchId;

    
    private Long hospitalId;

    
    private BigDecimal amount;

    
    private Integer status;

    
    private LocalDateTime payTime;

    /** 拒绝拨付理由 */
    @TableField("reject_reason")
    private String rejectReason;

    private LocalDateTime createTime;
}
