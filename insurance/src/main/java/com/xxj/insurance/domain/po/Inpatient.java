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

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("inpatient")
public class Inpatient implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long visitId;
    private Long userId;
    private Long hospitalId;
    private String inpatientNo;
    private String bedNo;
    private LocalDateTime admissionTime;
    private LocalDateTime dischargeTime;
    private BigDecimal depositTotal;
    private BigDecimal totalFee;
    /** 0-住院中 1-已出院 2-出院结算中 */
    private Integer status;
    private LocalDateTime createTime;
}
