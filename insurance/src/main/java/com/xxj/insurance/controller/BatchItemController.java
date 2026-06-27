package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.IBatchItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

// 申报明细管理：医保局和医院查询申报明细
@RestController
@RequestMapping("/batch-item")
@RequiredArgsConstructor
public class BatchItemController {

    private final IBatchItemService batchItemService;

    // 医保局查询申报明细，支持按批次ID和医院ID筛选
    @OperationLog("查询申报明细（医保局）")
    @GetMapping("/medical/list")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result medicalList(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long hospitalId) {
        return batchItemService.medicalList(batchId, hospitalId);
    }

    // 医院查询本院申报明细，支持按批次ID和时间范围筛选
    @OperationLog("查询申报明细（医院）")
    @GetMapping("/hospital/list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result hospitalList(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long hospitalId = UserHolder.requireHospitalId();
        return batchItemService.hospitalList(hospitalId, batchId, startTime, endTime);
    }

    // 根据结算单ID查询所属批次信息
    @OperationLog("查询结算单所属批次")
    @GetMapping("/by-settle/{settleId}")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result getBySettleId(@PathVariable Long settleId) {
        return batchItemService.getBySettleId(settleId);
    }

    // 分页查询指定批次下的结算单明细，支持按加入时间筛选
    @OperationLog("分页查询批次结算单明细")
    @GetMapping("/batch/{batchId}/page")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result listByBatchPage(@PathVariable Long batchId,
                                  @Valid PageDTO pageDTO,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return batchItemService.listByBatchPage(batchId, pageDTO, startTime, endTime);
    }
}
