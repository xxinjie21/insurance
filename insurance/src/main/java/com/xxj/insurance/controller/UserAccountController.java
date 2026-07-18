package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.AccountPayDTO;
import com.xxj.insurance.domain.dto.RechargeDTO;
import com.xxj.insurance.service.IUserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Permission({Role.PATIENT, Role.ADMIN})
public class UserAccountController {

    private final IUserAccountService userAccountService;

    // 查询当前用户账户信息
    @OperationLog("查询账户信息")
    @GetMapping("/get")
    public Result getAccount() {
        Long userId = UserHolder.getUserId();
        return userAccountService.getAccount(userId);
    }

    // 账户充值（仅管理员可操作，模拟医保个人账户划入）
    @OperationLog("账户充值")
    @PostMapping("/recharge")
    @Permission({Role.ADMIN})
    public Result recharge(@Valid @RequestBody RechargeDTO dto) {
        Long userId = UserHolder.getUserId();
        return userAccountService.recharge(userId, dto);
    }

    // 用户支付结算单自付部分
    @OperationLog("支付结算单")
    @PostMapping("/pay")
    public Result pay(@Valid @RequestBody AccountPayDTO dto) {
        Long userId = UserHolder.getUserId();
        return userAccountService.pay(userId, dto.getVisitId(), dto.getRemark());
    }

    // 查询充值记录列表（分页）
    @OperationLog("查询充值记录")
    @GetMapping("/recharge/list")
    public Result rechargeList(@Valid PageDTO pageDTO,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = UserHolder.getUserId();
        return userAccountService.rechargeList(userId, pageDTO, startTime, endTime);
    }

    // 查询消费记录列表（分页）
    @OperationLog("查询消费记录")
    @GetMapping("/consumption/list")
    public Result consumptionList(@Valid PageDTO pageDTO,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = UserHolder.getUserId();
        return userAccountService.consumptionList(userId, pageDTO, startTime, endTime);
    }
}
