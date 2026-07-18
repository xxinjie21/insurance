package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Permission({Role.MEDICAL, Role.ADMIN})
public class ReportController {

    private final IReportService reportService;

    @OperationLog("基金收支报表")
    @GetMapping("/fund")
    public Result fundReport(@RequestParam Integer year, @RequestParam(required = false) Integer month) {
        return reportService.fundReport(year, month);
    }

    @OperationLog("费用构成分析")
    @GetMapping("/fee-composition")
    public Result feeComposition(@RequestParam Integer year, @RequestParam(required = false) Integer month) {
        return reportService.feeCompositionReport(year, month);
    }

    @OperationLog("就诊统计报表")
    @GetMapping("/visit-stats")
    public Result visitStats(@RequestParam Integer year, @RequestParam(required = false) Integer month) {
        return reportService.visitStatsReport(year, month);
    }
}
