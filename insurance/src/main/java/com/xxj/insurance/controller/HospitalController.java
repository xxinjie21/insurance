package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.service.IHospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 医院管理 Controller - 处理医院注册、查询等业务
 * 核心功能：
 * - 医院注册（签约）
 * - 医院列表查询（患者/医保局/管理员可用）
 * - 医院查询本院患者
 */
@RestController
@RequestMapping("/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final IHospitalService hospitalService;

    /**
     * 医院注册（公开接口，无需登录）
     * 提交后状态为待审批(status=0)，需医保局审批通过后方可使用
     */
    @OperationLog("医院注册")
    @PostMapping("/sign")
    public Result sign(@RequestBody @Valid HospitalDTO hospitalDTO) {
        return hospitalService.sign(hospitalDTO);
    }

    /**
     * 获取所有医院列表（分页）
     * 支持按医院名称模糊搜索
     * 患者、医院、医保局和管理员可以查询，用于选择医院
     */
    @OperationLog("查询医院列表")
    @GetMapping("/list")
    @Permission({Role.PATIENT, Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result listAll(@Valid PageDTO pageDTO,
                          @RequestParam(required = false) String name) {
        return hospitalService.listAll(pageDTO, name);
    }

    /**
     * 医院查询本院的患者列表
     * hospitalId 从 Token 获取，确保只能查询本院患者
     */
    @OperationLog("查询本院患者列表")
    @GetMapping("/patient/list")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result listMyPatient(@Valid PageDTO pageDTO) {
        Long hospitalId = UserHolder.requireHospitalId();
        return hospitalService.listMyPatient(hospitalId, pageDTO);
    }

    /**
     * 审批通过医院
     * 仅限医保局或管理员操作
     */
    @OperationLog("审批通过医院")
    @PostMapping("/approve/{hospitalId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result approveHospital(@PathVariable Long hospitalId) {
        return hospitalService.approveHospital(hospitalId);
    }

    /**
     * 拒绝医院注册
     * 仅限医保局或管理员操作
     */
    @OperationLog("拒绝医院注册")
    @PostMapping("/reject/{hospitalId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result rejectHospital(@PathVariable Long hospitalId) {
        return hospitalService.rejectHospital(hospitalId);
    }

    @OperationLog("启用医院")
    @PostMapping("/enable/{hospitalId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result enableHospital(@PathVariable Long hospitalId) {
        return hospitalService.enableHospital(hospitalId);
    }

    @OperationLog("禁用医院")
    @PostMapping("/disable/{hospitalId}")
    @Permission({Role.MEDICAL, Role.ADMIN})
    public Result disableHospital(@PathVariable Long hospitalId) {
        return hospitalService.disableHospital(hospitalId);
    }

    /**
     * 管理员选择医院
     * 管理员选择要操作的医院后，将 hospitalId 存入 Redis
     * 后续请求中拦截器会自动将此 hospitalId 设入 UserHolder
     * 有效期与登录 token 一致（24小时）
     */
    @OperationLog("选择医院")
    @PostMapping("/select/{hospitalId}")
    @Permission({Role.ADMIN})
    public Result selectHospital(@PathVariable Long hospitalId) {
        // 管理员选择要操作的医院
        return hospitalService.selectHospital(hospitalId);
    }

    /**
     * 管理员取消选择医院
     */
    @OperationLog("取消选择医院")
    @PostMapping("/unselect")
    @Permission({Role.ADMIN})
    public Result unselectHospital() {
        // 管理员取消选中的医院
        return hospitalService.unselectHospital();
    }

    /**
     * 管理员获取当前选中的医院 ID
     */
    @OperationLog("获取已选医院")
    @GetMapping("/selected")
    @Permission({Role.ADMIN})
    public Result getSelectedHospital() {
        // 管理员获取当前选中的医院
        return hospitalService.getSelectedHospital();
    }

}
