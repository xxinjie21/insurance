package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.service.IAuditService;
import com.xxj.insurance.service.ISettleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Permission({Role.MEDICAL, Role.ADMIN})
public class AuditController {

    private final IAuditService auditService;
    private final ISettleService settleService;

    /** 审核单笔结算单，返回问题列表 */
    @OperationLog("审核结算单")
    @GetMapping("/settle/{settleId}")
    public Result auditSettle(@PathVariable Long settleId) {
        Settle settle = settleService.getById(settleId);
        if (settle == null) return Result.fail("结算单不存在");
        return Result.ok(auditService.auditSettle(settle));
    }
}
