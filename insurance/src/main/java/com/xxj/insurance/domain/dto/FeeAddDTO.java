package com.xxj.insurance.domain.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

@Data
public class FeeAddDTO {
    @NotNull(message = "就诊ID不能为空")
    private Long visitId;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal price;

    @NotNull(message = "数量不能为空")
    private Integer num;

    @NotNull(message = "费用类型不能为空")
    private Integer type;
}
