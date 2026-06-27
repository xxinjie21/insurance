package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Batch;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * 批次申报服务接口 - 处理医院向医保局申报结算单的业务
 */
public interface IBatchService extends IService<Batch> {

    /**
     * 创建申报批次
     * 生成唯一的批次号，用于打包多个结算单
     * 使用分布式锁防止并发创建
     *
     * @param hospitalId 医院 ID
     * @return 批次信息
     */
    Result createBatch(Long hospitalId);

    /**
     * 添加结算单到批次
     * 一个结算单只能添加到一个批次中，使用幂等性控制防止重复添加
     *
     * @param batchId 批次 ID
     * @param settleId 结算单 ID
     * @return 操作结果
     */
    Result addSettleToBatch(Long batchId, Long settleId);

    /**
     * 查询批次详情（包含批次信息和明细列表）
     * @param batchId 批次 ID
     * @return 批次详情
     */
    Result getBatchDetail(Long batchId);

    /**
     * 医院查询本院的批次列表（分页）
     * 支持按批次ID和创建时间范围筛选
     * @param hospitalId 医院 ID
     * @param pageDTO 分页参数
     * @param batchId 批次 ID（可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime 创建时间截止（可选）
     * @return 批次列表
     */
    Result hospitalList(Long hospitalId, PageDTO pageDTO, Long batchId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 医保局/管理员查询所有批次列表（分页）
     * 支持按批次ID和创建时间范围筛选，显示医院名称
     * @param pageDTO 分页参数
     * @param batchId 批次 ID（可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime 创建时间截止（可选）
     * @return 批次列表
     */
    Result medicalList(PageDTO pageDTO, Long batchId, Long hospitalId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 申报批次（将状态从"待申报"改为"已申报"）
     * @param batchId 批次 ID
     * @return 操作结果
     */
    Result declareBatch(Long batchId);

    /**
     * 撤回申报（医保局拨付前，将已申报批次恢复为待申报）
     * @param batchId 批次 ID
     * @return 操作结果
     */
    Result withdrawBatch(Long batchId);

    /**
     * 删除未申报的批次
     * 只能删除待申报状态的批次，同时删除关联的批次明细并恢复结算单状态
     * @param batchId 批次 ID
     * @return 操作结果
     */
    Result deleteBatch(Long batchId);

    /**
     * 查询当前医院的待申报批次列表（不分页，用于结算单选择批次）
     * @param hospitalId 医院 ID
     * @return 待申报批次列表
     */
    Result pendingList(Long hospitalId);
}
