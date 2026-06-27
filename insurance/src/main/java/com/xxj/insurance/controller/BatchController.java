package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

// 批次管理：创建、申报、查询、删除等操作
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final IBatchService batchService;

    // 医院创建批次，状态初始为"待申报"
    @OperationLog("创建批次")
    @PostMapping("/create")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result create() {
        Long hospitalId = UserHolder.requireHospitalId();
        return batchService.createBatch(hospitalId);
    }

    // 将结算单加入批次，更新结算单状态为"已申报"
    @OperationLog("添加结算单到批次")
    @PostMapping("/add-settle/{batchId}/{settleId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result addSettleToBatch(@PathVariable Long batchId,
                                   @PathVariable Long settleId) {
        return batchService.addSettleToBatch(batchId, settleId);
    }

    // 查询批次详情及包含的结算单列表
    @OperationLog("查询批次详情")
    @GetMapping("/detail/{batchId}")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result getBatchDetail(@PathVariable Long batchId) {
        return batchService.getBatchDetail(batchId);
    }

    // 医院分页查询本院批次，支持批次ID和时间范围筛选
    @OperationLog("查询本院批次列表")
    @GetMapping("/hospital/list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result hospitalList(@Valid PageDTO pageDTO,
                               @RequestParam(required = false) Long batchId,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long hospitalId = UserHolder.requireHospitalId();
        return batchService.hospitalList(hospitalId, pageDTO, batchId, startTime, endTime);
    }

    // 医保局/管理员分页查询所有批次，支持多条件筛选
    @OperationLog("查询所有批次列表")
    @GetMapping("/medical/list")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result medicalList(@Valid PageDTO pageDTO,
                              @RequestParam(required = false) Long batchId,
                              @RequestParam(required = false) Long hospitalId,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return batchService.medicalList(pageDTO, batchId, hospitalId, startTime, endTime);
    }

    // 申报批次，将批次状态改为"已申报"
    @OperationLog("申报批次")
    @PostMapping("/declare/{batchId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result declareBatch(@PathVariable Long batchId) {
        return batchService.declareBatch(batchId);
    }

    // 撤回批次申报（拨付或拒绝前可撤回）
    @OperationLog("撤回批次申报")
    @PostMapping("/withdraw/{batchId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result withdrawBatch(@PathVariable Long batchId) {
        return batchService.withdrawBatch(batchId);
    }

    // 查询本院待申报批次列表（供结算单加入时选择）
    @OperationLog("查询待申报批次")
    @GetMapping("/pending-list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result pendingList() {
        Long hospitalId = UserHolder.requireHospitalId();
        return batchService.pendingList(hospitalId);
    }

    // 医院删除未申报的批次，恢复关联结算单状态
    @OperationLog("删除批次")
    @DeleteMapping("/{batchId}")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result deleteBatch(@PathVariable Long batchId) {
        return batchService.deleteBatch(batchId);
    }

}
