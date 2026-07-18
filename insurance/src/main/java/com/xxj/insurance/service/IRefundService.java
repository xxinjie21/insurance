package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Refund;

import java.math.BigDecimal;

public interface IRefundService extends IService<Refund> {
    /** 申请退款（按比例拆分到各方） */
    Result apply(Long settleId, Long userId, BigDecimal refundAmount, String reason);
    /** 审批通过（执行原路退回） */
    Result approve(Long refundId);
    /** 拒绝退款 */
    Result reject(Long refundId, String reason);
    /** 退款列表 */
    Result listBySettle(Long settleId);
}
