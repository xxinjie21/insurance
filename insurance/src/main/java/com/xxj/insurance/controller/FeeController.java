package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.service.IFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/fee")
@RequiredArgsConstructor
@Permission({Role.HOSPITAL, Role.ADMIN})
public class FeeController {

    private final IFeeService feeService;

    // 批量添加费用明细
    @OperationLog("批量添加费用明细")
    @PostMapping("/batch/add")
    public Result batchAdd(@Valid @RequestBody List<@Valid FeeAddDTO> dtoList) {
        return feeService.batchAdd(dtoList);
    }

    // 按就诊ID查询费用明细列表
    @OperationLog("按就诊查询费用明细")
    @GetMapping("/listByVisitId")
    @Permission({Role.PATIENT, Role.HOSPITAL, Role.ADMIN})
    public Result listByVisitId(@RequestParam Long visitId) {
        return feeService.listByVisitId(visitId);
    }

    // 删除指定费用明细
    @OperationLog("删除费用明细")
    @DeleteMapping("/{feeId}")
    public Result deleteFee(@PathVariable Long feeId) {
        return feeService.deleteFee(feeId);
    }

    // 患者查询自己的费用明细列表（分页）
    @OperationLog("查询个人费用明细")
    @GetMapping("/my/list")
    @Permission({Role.PATIENT, Role.ADMIN})
    public Result myList(@Valid PageDTO pageDTO,
                         @RequestParam(required = false) Long visitId,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = UserHolder.getUserId();
        return feeService.myList(userId, pageDTO, visitId, startTime, endTime);
    }
}
