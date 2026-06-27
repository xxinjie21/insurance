package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Pay;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 基金拨付服务接口 - 处理医保局对医院申报批次的基金拨付业务
 */
public interface IPayService extends IService<Pay> {

    /**
     * 拨付批次款项
     * 医保局审核通过后拨付基金，更新批次状态为"已完成"
     * 使用分布式锁防止重复拨付
     * 
     * @param batchId 批次 ID
     * @return 拨付结果
     */
    Result payBatch(Long batchId);

    /**
     * 查询拨付信息
     * 根据批次 ID 查询拨付记录
     * 
     * @param batchId 批次 ID
     * @return 拨付信息
     */
    Result getPayByBatchId(Long batchId);

    /**
     * 拒绝拨付
     * @param batchId 批次 ID
     * @param reason 拒绝理由
     * @return 拨付记录
     */
    Result rejectBatchPay(Long batchId, String reason);
}
