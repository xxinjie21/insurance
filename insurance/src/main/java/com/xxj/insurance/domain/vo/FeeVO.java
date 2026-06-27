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
    private String hospitalName;
    private LocalDateTime createTime;
}