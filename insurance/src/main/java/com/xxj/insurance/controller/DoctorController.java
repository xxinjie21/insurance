package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
@Permission({Role.HOSPITAL, Role.ADMIN})
public class DoctorController {

    private final IDoctorService doctorService;

    @OperationLog("查询本院医生")
    @GetMapping("/list")
    public Result list(PageDTO pageDTO, @RequestParam(required = false) String keyword) {
        return doctorService.listByHospital(UserHolder.requireHospitalId(), pageDTO, keyword);
    }
}
