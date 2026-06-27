package com.xxj.insurance.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 拒绝拨付请求
 */
@Data
public class PayRejectDTO {

    @NotBlank(message = "拒绝理由不能为空")
    @Size(max = 500, message = "拒绝理由不能超过500字")
    private String reason;
}
