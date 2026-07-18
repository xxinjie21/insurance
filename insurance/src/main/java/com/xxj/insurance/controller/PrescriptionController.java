package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final IPrescriptionService prescriptionService;

    public static class PrescribeRequest {
        @NotNull public Long visitId;
        @NotNull public Long doctorId;
    }

    public static class ReviewRequest {
        @NotBlank public String reason;
    }

    @OperationLog("医生开方")
    @PostMapping("/prescribe")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result prescribe(@Valid @RequestBody PrescribeRequest req) {
        return prescriptionService.prescribe(req.visitId, req.doctorId);
    }

    @OperationLog("审核通过处方")
    @PostMapping("/approve/{prescriptionId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result approve(@PathVariable Long prescriptionId) {
        return prescriptionService.approve(prescriptionId);
    }

    @OperationLog("驳回处方")
    @PostMapping("/reject/{prescriptionId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result reject(@PathVariable Long prescriptionId, @Valid @RequestBody ReviewRequest req) {
        return prescriptionService.reject(prescriptionId, 1L, req.reason); // pharmacistId 后续接入登录用户
    }

    @OperationLog("查询处方列表")
    @GetMapping("/list/{visitId}")
    @Permission({Role.HOSPITAL, Role.PATIENT, Role.ADMIN})
    public Result listByVisit(@PathVariable Long visitId) {
        return prescriptionService.listByVisit(visitId);
    }
}
