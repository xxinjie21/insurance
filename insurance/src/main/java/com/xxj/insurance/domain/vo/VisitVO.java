package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitVO {
    private Long id;
    private Long userId;
    private String userName;
    /** 患者身份证号 */
    private String patientIdCard;
    /** 与 patientIdCard 相同，兼容前端 idCard 字段 */
    private String idCard;
    private Long hospitalId;
    private String hospitalName;
    private Integer type;
    private String diagnosis;
    private Integer status;
    private LocalDateTime createTime;
}