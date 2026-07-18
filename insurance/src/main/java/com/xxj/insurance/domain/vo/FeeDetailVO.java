package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算单费用明细 VO — 对标真实医保结算单的逐项拆分
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
public class FeeDetailVO {
    /** 费用ID */
    private Long id;
    /** 项目名称 */
    private String name;
    /** 医保编码 */
    private String insuranceCode;
    /** 规格 */
    private String specification;
    /** 数量 */
    private Integer num;
    /** 单价 */
    private BigDecimal price;
    /** 小计金额 */
    private BigDecimal total;
    /** 费用类别：1-甲类 2-乙类 3-自费 */
    private Integer type;
    /** 本项报销金额(统筹支付) */
    private BigDecimal reimburse;
    /** 本项自付金额 */
    private BigDecimal selfPay;
}
