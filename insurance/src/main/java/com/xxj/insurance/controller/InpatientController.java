package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IInpatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RestController
@RequestMapping("/inpatient")
@RequiredArgsConstructor
public class InpatientController {

    private final IInpatientService inpatientService;

    public static class AdmitRequest {
        @NotNull public Long visitId;
        @NotNull public Long userId;
        @NotNull public Long hospitalId;
        public String bedNo;
    }

    public static class DepositRequest {
        @NotNull public Long inpatientId;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
        public String remark;
    }

    @OperationLog("入院登记")
    @PostMapping("/admit")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result admit(@Valid @RequestBody AdmitRequest req) {
        return inpatientService.admit(req.visitId, req.userId, req.hospitalId, req.bedNo);
    }

    @OperationLog("缴纳住院押金")
    @PostMapping("/deposit")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result deposit(@Valid @RequestBody DepositRequest req) {
        return inpatientService.deposit(req.inpatientId, req.amount, req.remark);
    }

    @OperationLog("出院结算")
    @PostMapping("/discharge/{inpatientId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result discharge(@PathVariable Long inpatientId) {
        return inpatientService.discharge(inpatientId);
    }

    @OperationLog("查询住院列表")
    @GetMapping("/hospital/list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result hospitalList(PageDTO pageDTO) {
        return inpatientService.hospitalList(UserHolder.requireHospitalId(), pageDTO);
    }

    @OperationLog("查询个人住院记录")
    @GetMapping("/my/list")
    @Permission({Role.PATIENT, Role.ADMIN})
    public Result myList(PageDTO pageDTO) {
        return inpatientService.myList(UserHolder.getUserId(), pageDTO);
    }
}
