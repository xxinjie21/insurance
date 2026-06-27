package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Permission({Role.PATIENT, Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
public class DashboardController {

    private final IDashboardService dashboardService;

    // 获取统计数据
    @OperationLog("查询统计数据")
    @GetMapping("/stats")
    public Result getStats(@RequestParam(required = false, defaultValue = "1") Integer role) {
        return dashboardService.getStats(role);
    }
}
