package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.BatchItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 * 申报明细表 服务类
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
public interface IBatchItemService extends IService<BatchItem> {

    /**
     * 医保局查询申报明细列表
     * @param batchId 批次 ID（可选）
     * @param hospitalId 医院 ID（可选）
     * @return 申报明细列表
     */
    Result medicalList(Long batchId, Long hospitalId);

    /**
     * 医院查询本院申报明细列表
     * @param hospitalId 医院 ID
     * @param batchId 批次 ID（可选）
     * @param startTime 批次创建时间起（可选）
     * @param endTime 批次创建时间止（可选）
     * @return 申报明细列表
     */
    Result hospitalList(Long hospitalId, Long batchId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询指定批次下的申报明细（结算单）
     */
    Result listByBatchPage(Long batchId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime);

    Result getBySettleId(Long settleId);
}
