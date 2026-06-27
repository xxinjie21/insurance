package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.domain.po.Fee;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用明细服务接口 - 处理就诊费用明细的添加和查询
 */
public interface IFeeService extends IService<Fee> {

    /**
     * 批量添加费用明细
     * 医院为就诊记录添加多个费用项目，自动计算总价
     * @param dtoList 费用明细列表（包含项目名称、类型、单价、数量）
     * @return 添加成功的费用列表
     */
    Result batchAdd(List<FeeAddDTO> dtoList);

    /**
     * 根据就诊 ID 查询费用明细列表
     * @param visitId 就诊 ID
     * @return 费用明细列表
     */
    Result listByVisitId(Long visitId);

    /**
     * 患者查询自己的费用明细列表（分页）
     * @param userId 患者 ID（从 Token 获取）
     * @param pageDTO 分页参数
     * @param visitId 就诊 ID（可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime 创建时间截止（可选）
     * @return 费用明细分页列表（含就诊医院名称）
     */
    Result myList(Long userId, PageDTO pageDTO, Long visitId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除费用明细
     * 仅未结算就诊的费用可删除，且只能删除本院的费用
     * @param feeId 费用 ID
     * @return 操作结果
     */
    Result deleteFee(Long feeId);
}
