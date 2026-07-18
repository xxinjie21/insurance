package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IYearEndService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/year-end")
@RequiredArgsConstructor
@Permission({Role.MEDICAL, Role.ADMIN})
public class YearEndController {

    private final IYearEndService yearEndService;

    @OperationLog("年度结转")
    @PostMapping("/rollover")
    public Result rollover() {
        return yearEndService.rollover();
    }

    @OperationLog("年度对账报表")
    @GetMapping("/reconcile")
    public Result reconcileReport(@RequestParam(required = false) Integer year) {
        return yearEndService.reconcileReport(year);
    }
}
