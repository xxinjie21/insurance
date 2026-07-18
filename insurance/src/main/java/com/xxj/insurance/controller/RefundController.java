package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
public class RefundController {

    private final IRefundService refundService;

    public static class ApplyRequest {
        @NotNull public Long settleId;
        @NotNull @DecimalMin("0.01") public BigDecimal refundAmount;
        @NotBlank public String reason;
    }

    public static class RejectRequest {
        @NotBlank public String reason;
    }

    @OperationLog("申请退款")
    @PostMapping("/apply")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result apply(@Valid @RequestBody ApplyRequest req) {
        return refundService.apply(req.settleId, UserHolder.getUserId(), req.refundAmount, req.reason);
    }

    @OperationLog("审批退款通过")
    @PostMapping("/approve/{refundId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result approve(@PathVariable Long refundId) {
        return refundService.approve(refundId);
    }

    @OperationLog("拒绝退款")
    @PostMapping("/reject/{refundId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result reject(@PathVariable Long refundId, @Valid @RequestBody RejectRequest req) {
        return refundService.reject(refundId, req.reason);
    }

    @OperationLog("查询退款")
    @GetMapping("/list/{settleId}")
    public Result listBySettle(@PathVariable Long settleId) {
        return refundService.listBySettle(settleId);
    }
}
