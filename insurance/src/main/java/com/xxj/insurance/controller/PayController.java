package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.domain.dto.PayRejectDTO;
import com.xxj.insurance.service.IPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

// 基金拨付管理：医保局对申报批次进行拨付或拒绝
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
@Permission({Role.MEDICAL, Role.ADMIN})
public class PayController {

    private final IPayService payService;

    // 医保局拨付批次款项，拨付后批次状态变为"已完成"
    @OperationLog("拨付批次款项")
    @PostMapping("/pay-batch/{batchId}")
    public Result payBatch(@PathVariable Long batchId) {
        return payService.payBatch(batchId);
    }

    // 根据批次ID查询拨付信息
    @OperationLog("查询拨付信息")
    @GetMapping("/by-batch/{batchId}")
    public Result getPayByBatchId(@PathVariable Long batchId) {
        return payService.getPayByBatchId(batchId);
    }

    // 拒绝拨付并填写拒绝理由
    @OperationLog("拒绝拨付")
    @PostMapping("/reject-batch/{batchId}")
    public Result rejectBatchPay(@PathVariable Long batchId, @Valid @RequestBody PayRejectDTO dto) {
        return payService.rejectBatchPay(batchId, dto.getReason());
    }
}
