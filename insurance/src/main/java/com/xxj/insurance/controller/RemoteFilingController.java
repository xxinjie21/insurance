package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IRemoteFilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/remote-filing")
@RequiredArgsConstructor
@Permission({Role.PATIENT, Role.ADMIN})
public class RemoteFilingController {

    private final IRemoteFilingService filingService;

    public static class FileRequest {
        @NotBlank public String insuredCity;
        @NotBlank public String treatmentCity;
        public Long treatmentHospitalId;
    }

    @OperationLog("异地就医备案")
    @PostMapping("/file")
    public Result file(@Valid @RequestBody FileRequest req) {
        return filingService.file(UserHolder.getUserId(), req.insuredCity, req.treatmentCity, req.treatmentHospitalId);
    }

    @OperationLog("取消备案")
    @PostMapping("/cancel/{filingId}")
    public Result cancel(@PathVariable Long filingId) {
        return filingService.cancel(filingId);
    }

    @OperationLog("查询备案")
    @GetMapping("/my")
    public Result myFilings() {
        return filingService.myFilings(UserHolder.getUserId());
    }
}
