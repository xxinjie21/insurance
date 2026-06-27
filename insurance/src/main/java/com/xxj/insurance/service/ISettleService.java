package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;

import java.time.LocalDateTime;
import com.xxj.insurance.domain.po.Settle;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医保结算服务接口 - 处理就诊费用的医保报销计算
 */
public interface ISettleService extends IService<Settle> {

    /**
     * 就诊医保结算
     * 根据费用明细计算报销金额：
     * - 甲类费用：100% 报销
     * - 乙类费用：80% 报销
     * - 自费费用：0% 报销
     * 使用分布式锁防止重复结算
     *
     * @param visitId 就诊 ID
     * @return 结算结果（包含总费用、报销金额、自付金额）
     */
    Result calculate(Long visitId);

    /**
     * 查询就诊的结算详情
     * @param visitId 就诊 ID
     * @return 结算信息
     */
    Result getSettleDetail(Long visitId);

    /**
     * 患者查询自己的结算单列表（分页）
     * @param userId 患者 ID（从 Token 获取）
     * @param pageDTO 分页参数
     * @param hospitalId 医院 ID（可选，筛选指定医院的结算单）
     * @param startTime 结算时间起始（可选）
     * @param endTime 结算时间截止（可选）
     * @return 结算单列表
     */
    Result myList(Long userId, PageDTO pageDTO, Long hospitalId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 医院查询本院的结算单列表（分页）
     * @param hospitalId 医院 ID（从 Token 获取）
     * @param pageDTO 分页参数
     * @return 结算单列表
     */
    Result hospitalList(Long hospitalId, PageDTO pageDTO, String patientName, Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询本院可添加到批次的结算单（未申报状态）
     * @param hospitalId 医院 ID
     * @return 结算单列表（含患者姓名）
     */
    Result availableForBatch(Long hospitalId);
}
