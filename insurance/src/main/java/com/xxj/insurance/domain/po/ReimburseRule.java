package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 医保报销规则表
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("reimburse_rule")
public class ReimburseRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 参保类型：1-职工 2-居民 */
    private Integer insuranceType;

    /** 医院等级：1-三甲 2-三乙 3-二甲 4-二乙 5-一级 6-社区 */
    private Integer hospitalLevel;

    /** 就诊类型：1-门诊 2-住院 */
    private Integer visitType;

    /** 起付线 */
    private BigDecimal deductible;

    /** 统筹报销比例(0~1) */
    private BigDecimal reimburseRatio;

    /** 年度封顶线 */
    private BigDecimal annualCap;

    /** 乙类先自付比例(0~1) */
    private BigDecimal categoryBSelfRatio;
}
