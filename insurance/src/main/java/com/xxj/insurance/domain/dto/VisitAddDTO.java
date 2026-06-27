package com.xxj.insurance.domain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class VisitAddDTO {
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    private Long hospitalId; // 由 Controller 从 Token 填入，不需要前端传

    @NotNull(message = "就诊类型不能为空")
    private Integer type;

    @NotBlank(message = "诊断结果不能为空")
    private String diagnosis;
}
