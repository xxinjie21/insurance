package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchVO {
    // 批次基本信息
    private Long id;
    private String batchNo;
    private Long hospitalId;
    private String hospitalName;
    private Integer settleCnt;
    private BigDecimal totalAmt;
    private Integer status;
    private LocalDateTime createTime;
    private List<BatchItemVO> batchItems;
    /** 拒绝拨付理由（批次状态为拨付拒绝时有值） */
    private String rejectReason;
}