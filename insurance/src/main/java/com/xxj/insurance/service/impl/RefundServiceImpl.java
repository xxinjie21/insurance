package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.*;
import com.xxj.insurance.mapper.*;
import com.xxj.insurance.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> implements IRefundService {

    private final SettleMapper settleMapper;
    private final UserMapper userMapper;
    private final YearAccumulateMapper yearAccumulateMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Result apply(Long settleId, Long userId, BigDecimal refundAmount, String reason) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("退款金额必须大于0");
        }
        Settle settle = settleMapper.selectById(settleId);
        if (settle == null) return Result.fail("结算单不存在");
        if (refundAmount.compareTo(settle.getTotal()) > 0) return Result.fail("退款金额不能超过总费用");

        // 按比例拆分退款到各方
        BigDecimal total = settle.getTotal();
        BigDecimal ratio = refundAmount.divide(total, 6, RoundingMode.HALF_UP);

        BigDecimal poolingRefund = BigDecimal.ZERO;
        if (settle.getPoolingPay() != null) poolingRefund = settle.getPoolingPay().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal accountRefund = BigDecimal.ZERO;
        if (settle.getAccountPay() != null) accountRefund = settle.getAccountPay().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cashRefund = refundAmount.subtract(poolingRefund).subtract(accountRefund).setScale(2, RoundingMode.HALF_UP);

        // userId 由调用方传入
        Long actualUserId = userId;

        Refund refund = new Refund();
        refund.setSettleId(settleId);
        refund.setVisitId(settle.getVisitId());
        refund.setUserId(actualUserId);
        refund.setTotalRefund(refundAmount);
        refund.setPoolingRefund(poolingRefund);
        refund.setAccountRefund(accountRefund);
        refund.setCashRefund(cashRefund);
        refund.setReason(reason);
        refund.setStatus(0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        save(refund);

        log.info("退款申请提交，refundId:{}, settleId:{}, amount:{}", refund.getId(), settleId, refundAmount);
        return Result.ok(refund);
    }

    @Override
    public Result approve(Long refundId) {
        Refund refund = getById(refundId);
        if (refund == null) return Result.fail("退款不存在");
        if (refund.getStatus() != 0) return Result.fail("仅待审批状态可审批");

        return transactionTemplate.execute(status -> {
            refund.setStatus(1);
            updateById(refund);

            // 统筹部分退款：回退年度累计
            if (refund.getPoolingRefund().compareTo(BigDecimal.ZERO) > 0) {
                int year = LocalDateTime.now().getYear();
                YearAccumulate accumulate = yearAccumulateMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<YearAccumulate>()
                                .eq(YearAccumulate::getUserId, refund.getUserId())
                                .eq(YearAccumulate::getYear, year));
                if (accumulate != null) {
                    accumulate.setPoolingTotal(accumulate.getPoolingTotal().subtract(refund.getPoolingRefund()));
                    if (accumulate.getPoolingTotal().compareTo(BigDecimal.ZERO) < 0) accumulate.setPoolingTotal(BigDecimal.ZERO);
                    yearAccumulateMapper.updateById(accumulate);
                }
            }

            // 个账退款：退回个人账户余额
            if (refund.getAccountRefund().compareTo(BigDecimal.ZERO) > 0) {
                User user = userMapper.selectById(refund.getUserId());
                if (user != null && user.getPersonalAccountBalance() != null) {
                    user.setPersonalAccountBalance(user.getPersonalAccountBalance().add(refund.getAccountRefund()));
                    userMapper.updateById(user);
                }
            }

            // 更新结算单退款后金额
            Settle settle = settleMapper.selectById(refund.getSettleId());
            if (settle != null) {
                if (settle.getPoolingPay() != null) settle.setPoolingPay(settle.getPoolingPay().subtract(refund.getPoolingRefund()));
                if (settle.getAccountPay() != null) settle.setAccountPay(settle.getAccountPay().subtract(refund.getAccountRefund()));
                if (settle.getCashPay() != null) settle.setCashPay(settle.getCashPay().subtract(refund.getCashRefund()));
                settle.setTotal(settle.getTotal().subtract(refund.getTotalRefund()));
                settleMapper.updateById(settle);
            }

            refund.setStatus(3); // 已完成退款
            updateById(refund);

            log.info("退款审批完成，refundId:{}, userId:{}", refundId, refund.getUserId());
            return Result.ok("退款完成");
        });
    }

    @Override
    public Result reject(Long refundId, String reason) {
        Refund refund = getById(refundId);
        if (refund == null) return Result.fail("退款不存在");
        if (refund.getStatus() != 0) return Result.fail("仅待审批状态可拒绝");
        refund.setStatus(2);
        refund.setRejectReason(reason);
        updateById(refund);
        return Result.ok("已拒绝");
    }

    @Override
    public Result listBySettle(Long settleId) {
        return Result.ok(lambdaQuery().eq(Refund::getSettleId, settleId)
                .orderByDesc(Refund::getCreateTime).list());
    }
}
