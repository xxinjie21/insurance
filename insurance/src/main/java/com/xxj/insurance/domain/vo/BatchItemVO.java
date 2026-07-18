package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申报明细 VO - 包含关联的结算单、患者和批次信息
 */
@Data
public class BatchItemVO {
    private Long id;
    private Long batchId;
    private Long settleId;
    private Integer audit;
    /** 审核调减金额 */
    private BigDecimal adjustAmount;

    // 关联结算单信息
    private BigDecimal settleTotal;
    private BigDecimal settleReimburse;
    private BigDecimal settleSelfPay;
    private Integer settleStatus;

    // 关联患者信息
    private String patientName;
    private String patientIdCard;
    /** 与 patientIdCard 相同，便于前端使用 idCard 字段 */
    private String idCard;

    // 关联批次信息
    private String batchNo;
    private Integer batchStatus;
    private LocalDateTime batchCreateTime;

    private LocalDateTime createTime;
}
