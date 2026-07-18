package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 参保人年度累计表
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("year_accumulate")
public class YearAccumulate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 参保人ID */
    private Long userId;

    /** 年份 */
    private Integer year;

    /** 本年度已累计起付线金额 */
    private BigDecimal deductibleUsed;

    /** 本年度统筹支付累计 */
    private BigDecimal poolingTotal;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
