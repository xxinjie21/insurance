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

    /** 医保项目编码 */
    private String insuranceCode;

    /** 药品规格 */
    private String specification;

    /** 用法用量 */
    private String usageMethod;

    /** 目录类型：drug/treatment/consumable（选目录时传入） */
    private String catalogType;

    /** 目录项ID（选目录时传入，后端自动回填名称/类型/编码/规格） */
    private Long catalogId;
}
