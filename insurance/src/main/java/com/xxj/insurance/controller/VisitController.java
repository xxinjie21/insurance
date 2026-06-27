package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.VisitAddDTO;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/visit")
@RequiredArgsConstructor
@Permission({Role.HOSPITAL, Role.ADMIN})
public class VisitController {

    private final IVisitService visitService;

    // 添加就诊记录
    @OperationLog("添加就诊记录")
    @PostMapping("/add")
    public Result add(@Valid @RequestBody VisitAddDTO dto) {
        Long hospitalId = UserHolder.requireHospitalId();
        dto.setHospitalId(hospitalId);
        return visitService.add(dto);
    }

    // 患者查询本人的就诊记录
    @OperationLog("查询个人就诊记录")
    @GetMapping("/my/list")
    @Permission({Role.PATIENT, Role.ADMIN})
    public Result myList(@Valid PageDTO pageDTO,
                         @RequestParam(required = false) Long hospitalId,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return visitService.myList(pageDTO, hospitalId, startTime, endTime);
    }

    // 医院查询本院的就诊记录
    @OperationLog("查询本院就诊记录")
    @GetMapping("/hospital/list")
    public Result hospitalList(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                               @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
                               @RequestParam(required = false) String patientName,
                               @RequestParam(required = false) Long userId,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long hospitalId = UserHolder.requireHospitalId();
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return visitService.hospitalList(hospitalId, pageDTO, patientName, userId, startTime, endTime);
    }

    // 根据ID获取就诊详情
    @OperationLog("查询就诊详情")
    @GetMapping("/{visitId}")
    public Result getVisitById(@PathVariable Long visitId) {
        return visitService.getVisitById(visitId);
    }

    // 删除就诊记录
    @OperationLog("删除就诊记录")
    @DeleteMapping("/{visitId}")
    public Result delete(@PathVariable Long visitId) {
        return visitService.delete(visitId);
    }

}
