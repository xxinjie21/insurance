package com.xxj.insurance.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 支付 DTO（使用账户余额支付）
 * 注意：支付金额从数据库结算单获取，前端不可传入，防止篡改
 */
@Data
public class AccountPayDTO {

    @NotNull(message = "就诊ID不能为空")
    private Long visitId;

    private String remark;
}
