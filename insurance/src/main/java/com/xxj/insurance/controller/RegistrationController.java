package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final IRegistrationService registrationService;

    public static class RegRequest {
        @NotBlank public String idCard;
        @NotNull public Long hospitalId;
        public String dept;
        public String doctorName;
        @NotNull public Integer regType;
    }

    @OperationLog("门诊挂号")
    @PostMapping("/add")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result register(@Valid @RequestBody RegRequest req) {
        return registrationService.register(req.idCard, req.hospitalId, req.dept, req.doctorName, req.regType);
    }

    @OperationLog("查询个人挂号记录")
    @GetMapping("/my/list")
    @Permission({Role.PATIENT, Role.ADMIN})
    public Result myList(PageDTO pageDTO, @RequestParam(required = false) Long hospitalId) {
        return registrationService.myList(pageDTO, hospitalId);
    }

    @OperationLog("查询本院挂号记录")
    @GetMapping("/hospital/list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result hospitalList(PageDTO pageDTO) {
        return registrationService.hospitalList(UserHolder.requireHospitalId(), pageDTO);
    }
}
