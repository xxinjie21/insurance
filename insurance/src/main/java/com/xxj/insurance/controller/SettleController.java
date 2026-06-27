package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.service.ISettleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 医保结算 Controller
 * 
 * 功能说明：
 * 1. 医保结算计算：根据就诊记录计算报销金额（甲类 100%、乙类 80%）
 * 2. 查询结算单详情：患者查询自己的结算单，医院查询本院的结算单
 * 3. 查询结算单列表：分页查询，支持按角色过滤
 * 
 * 核心特性：
 * - 幂等性控制：使用 Redis 防止重复结算
 * - 分布式锁：确保并发安全
 * - 数据隔离：患者只能查自己的，医院只能查本院的
 * 
 * 权限说明：
 * - 计算接口需要医院角色或管理员
 * - 患者查询需要患者角色或管理员
 * 
 * @author xxj
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/settle")
@RequiredArgsConstructor
@Permission({Role.HOSPITAL, Role.ADMIN}) // 默认需要医院角色或管理员
public class SettleController {

    private final ISettleService settleService;

    // 结算计算：根据就诊ID计算报销金额，生成结算单
    /**
     * 医保结算计算
     * 
     * 业务逻辑：
     * 1. 校验就诊记录是否存在且状态为"待结算"
     * 2. 遍历所有费用明细，计算报销金额
     * 3. 创建结算单，状态为"待申报"
     * 4. 更新就诊状态为"已结算"
     * 
     * 幂等性保证：
     * - Redis 预检查：快速拦截重复请求
     * - 分布式锁：防止并发结算
     * - 数据库校验：最终兜底
     * 
     * @param visitId 就诊记录 ID
     * @return 结算结果（包含报销金额、自付金额等）
     */
    @OperationLog("医保结算计算")
    @PostMapping("/calculate/{visitId}")
    public Result calculate(@PathVariable Long visitId) {
        return settleService.calculate(visitId);
    }

    // 查询结算单详情：按就诊ID获取结算信息
    /**
     * 查询结算单详情
     * 
     * 业务逻辑：
     * 1. 查询结算单基本信息
     * 2. 关联查询就诊记录、费用明细
     * 3. 校验权限：只能查询自己/本院的结算单
     * 
     * @param visitId 就诊记录 ID
     * @return 结算单详情
     */
    @OperationLog("查询结算单详情")
    @GetMapping("/detail/{visitId}")
    public Result getSettleDetail(@PathVariable Long visitId) {
        return settleService.getSettleDetail(visitId);
    }

    // 患者查询自己的结算单列表（分页）
    /**
     * 患者查询自己的结算单列表（分页）
     * 
     * 业务逻辑：
     * 1. 从 Token 获取患者 userId
     * 2. 通过就诊记录关联查询结算单
     * 
     * @param pageDTO 分页参数
     * @return 结算单列表
     */
    @OperationLog("查询个人结算单列表")
    @GetMapping("/my/list")
    @Permission({Role.PATIENT, Role.ADMIN}) // 患者角色或管理员可以访问
    public Result myList(@Valid PageDTO pageDTO,
                         @RequestParam(required = false) Long hospitalId,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = UserHolder.getUserId();
        return settleService.myList(userId, pageDTO, hospitalId, startTime, endTime);
    }

    // 医院查询本院的结算单列表（分页）
    /**
     * 医院查询本院的结算单列表（分页）
     * 
     * 业务逻辑：
     * 1. 从 Token 获取医院 hospitalId
     * 2. 查询本院的所有结算单
     * 
     * @param pageDTO 分页参数
     * @return 结算单列表
     */
    @OperationLog("查询本院结算单列表")
    @GetMapping("/hospital/list")
    @Permission({Role.HOSPITAL, Role.ADMIN}) // 医院角色或管理员可以访问
    public Result hospitalList(@Valid PageDTO pageDTO,
                             @RequestParam(required = false) String patientName,
                             @RequestParam(required = false) Long userId,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long hospitalId = UserHolder.requireHospitalId();
        return settleService.hospitalList(hospitalId, pageDTO, patientName, userId, startTime, endTime);
    }

    // 查询本院可添加到批次的结算单（未申报状态）
    /**
     * 查询本院可添加到批次的结算单（未申报状态）
     * 返回带患者姓名的结算单列表，供批次添加时选择
     */
    @OperationLog("查询可申报结算单")
    @GetMapping("/available-for-batch")
    @Permission({Role.HOSPITAL, Role.ADMIN})
    public Result availableForBatch() {
        Long hospitalId = UserHolder.requireHospitalId();
        return settleService.availableForBatch(hospitalId);
    }
}
