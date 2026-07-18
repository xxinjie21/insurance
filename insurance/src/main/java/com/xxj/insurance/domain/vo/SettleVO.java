package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算单 VO - 包含关联的患者和就诊信息
 */
@Data
public class SettleVO {
    private Long id;
    private Long visitId;
    private Long hospitalId;
    private BigDecimal total;
    private BigDecimal reimburse;
    private BigDecimal selfPay;
    /** 统筹支付金额 */
    private BigDecimal poolingPay;
    /** 个人账户支付金额 */
    private BigDecimal accountPay;
    /** 个人现金支付金额 */
    private BigDecimal cashPay;
    /** 大病保险支付金额 */
    private BigDecimal catastrophicPay;
    /** 医疗救助支付金额 */
    private BigDecimal assistancePay;
    private Integer status;
    private LocalDateTime createTime;

    // 关联信息
    private String patientName;
    /** 患者身份证号 */
    private String patientIdCard;
    /** 与 patientIdCard 相同，兼容前端 idCard 字段 */
    private String idCard;
    private String hospitalName;
    private Integer visitType;
    private String diagnosis;

    /** 费用明细列表（查询详情时填充） */
    private List<FeeDetailVO> feeDetails;
}
