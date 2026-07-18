package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeVO {
    private Long id;
    private Long visitId;
    private String name;
    private BigDecimal price;
    private Integer num;
    private BigDecimal total;
    private Integer type;
    /** 医保项目编码 */
    private String insuranceCode;
    /** 药品规格 */
    private String specification;
    /** 用法用量 */
    private String usageMethod;
    /** 费用日期（住院每日清单） */
    private java.time.LocalDate feeDate;
    /** 关联处方ID */
    private Long prescriptionId;
    private String hospitalName;
    private LocalDateTime createTime;
}