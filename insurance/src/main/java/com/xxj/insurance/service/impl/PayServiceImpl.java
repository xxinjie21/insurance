package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.BatchItem;
import com.xxj.insurance.domain.po.Pay;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.mapper.PayMapper;
import com.xxj.insurance.service.IBatchItemService;
import com.xxj.insurance.service.IBatchService;
import com.xxj.insurance.service.IPayService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.service.ISettleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 拨付 Service 实现类
 * 修复记录：
 * - 分布式锁 leaseTime: -1 改为 30 秒
 * - 幂等标识写入时机：移到事务提交成功后
 * - N+1 查询：批量查询结算单
 * - 事务回滚 NPE 兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl extends ServiceImpl<PayMapper, Pay> implements IPayService {

    private static final long LOCK_LEASE_TIME = 30;

    private final IBatchService batchService;
    private final IBatchItemService batchItemService;
    private final ISettleService settleService;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    // 医保局拨付批次：分布式锁 + 幂等防重
    @Override
    public Result payBatch(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        String lockKey = "lock:pay:batch:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:pay:batch:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 抢分布式锁，防止并发拨付
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // Redis 幂等校验：已拨付则快速拒绝
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已拨付，请勿重复操作");
            }

            Result result = transactionTemplate.execute(status -> executePayBatchWithTransaction(batchId));

            // 事务提交成功后才写幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("拨付操作被中断，批次 ID: {}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 事务内执行拨付逻辑（幂等校验、状态更新）
    public Result executePayBatchWithTransaction(Long batchId) {
        String idempotentKey = "idempotent:pay:batch:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已拨付，请勿重复拨付");
        }

        log.info("开始拨付批次，批次 ID: {}", batchId);

        // 数据库兜底校验：防止 Redis 失效时重复拨付
        LambdaQueryWrapper<Pay> payWrapper = new LambdaQueryWrapper<>();
        payWrapper.eq(Pay::getBatchId, batchId);
        Pay existPay = this.getOne(payWrapper);
        if (existPay != null && ReimburseConstants.PAY_STATUS_PAID.equals(existPay.getStatus())) {
            return Result.fail("该批次已拨付，请勿重复拨付");
        }
        if (existPay != null && ReimburseConstants.PAY_STATUS_REJECTED.equals(existPay.getStatus())) {
            return Result.fail("该批次已被拒绝拨付");
        }

        // 校验批次状态机：仅已申报可拨付
        Batch batch = batchService.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }
        if (ReimburseConstants.BATCH_STATUS_COMPLETED.equals(batch.getStatus())) {
            return Result.fail("该批次已完成，不能重复拨付");
        }
        if (ReimburseConstants.BATCH_STATUS_PAY_REJECTED.equals(batch.getStatus())) {
            return Result.fail("该批次已被拒绝拨付");
        }
        if (!ReimburseConstants.BATCH_STATUS_DECLARED.equals(batch.getStatus())) {
            return Result.fail("仅已申报状态的批次可拨付");
        }
        if (batch.getSettleCnt() == null || batch.getSettleCnt() <= 0) {
            return Result.fail("批次没有结算单，不能拨付");
        }
        if (batch.getTotalAmt() == null || batch.getTotalAmt().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("批次金额必须大于 0");
        }

        // 创建或更新拨付记录（可重复调用）
        Pay pay = existPay == null ? new Pay() : existPay;
        if (existPay == null) {
            pay.setBatchId(batchId);
            pay.setHospitalId(batch.getHospitalId());
            pay.setCreateTime(LocalDateTime.now());
        }
        pay.setStatus(ReimburseConstants.PAY_STATUS_PAID);
        pay.setAmount(batch.getTotalAmt());
        pay.setPayTime(LocalDateTime.now());
        if (existPay == null) {
            this.save(pay);
        } else {
            this.updateById(pay);
        }

        // 更新批次状态
        batch.setStatus(ReimburseConstants.BATCH_STATUS_COMPLETED);
        batchService.updateById(batch);

        // 批量查结算单，避免 N+1
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BatchItem::getBatchId, batchId);
        List<BatchItem> items = batchItemService.list(itemWrapper);

        List<Long> settleIds = items.stream()
                .map(BatchItem::getSettleId)
                .collect(Collectors.toList());

        if (!settleIds.isEmpty()) {
            List<Settle> settleList = settleService.listByIds(settleIds);
            for (Settle settle : settleList) {
                settle.setStatus(ReimburseConstants.SETTLE_STATUS_PAID);
            }
            settleService.updateBatchById(settleList);
        }

        // 注意：幂等标记已移到事务提交后（外层 payBatch 方法中）
        log.info("拨付成功，批次 ID: {}, 拨付 ID: {}", batchId, pay.getId());
        return Result.ok(pay);
    }

    // 查询某批次的拨付记录
    @Override
    public Result getPayByBatchId(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        LambdaQueryWrapper<Pay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pay::getBatchId, batchId);
        Pay pay = this.getOne(wrapper);

        if (pay == null) {
            return Result.fail("该批次暂无拨付记录");
        }

        return Result.ok(pay);
    }

    // 医保局拒绝拨付批次：分布式锁 + 幂等防重
    @Override
    public Result rejectBatchPay(Long batchId, String reason) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }
        if (StrUtil.isBlank(reason)) {
            return Result.fail("拒绝理由不能为空");
        }

        String lockKey = "lock:pay:batch:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:pay:reject:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 第一层拦截：Redis 快速判断
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已拒绝拨付，请勿重复提交");
            }

            // 抢分布式锁，防止并发操作
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 锁内执行业务（带事务）
            Result result = transactionTemplate.execute(status -> executeRejectBatchWithTransaction(batchId, reason.trim(), idempotentBucket));

            // 事务提交成功后才写入幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("拒绝拨付被中断，批次 ID: {}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 事务内执行拒绝拨付逻辑（三层幂等 + 状态更新）
    private Result executeRejectBatchWithTransaction(Long batchId, String reason, RBucket<String> idempotentBucket) {
        // 第二层拦截：锁内再次校验幂等
        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已拒绝拨付，请勿重复提交");
        }

        // 数据库校验：防止 Redis 失效时重复拒绝
        LambdaQueryWrapper<Pay> payWrapper = new LambdaQueryWrapper<>();
        payWrapper.eq(Pay::getBatchId, batchId);
        Pay existPay = this.getOne(payWrapper);
        if (existPay != null && ReimburseConstants.PAY_STATUS_PAID.equals(existPay.getStatus())) {
            return Result.fail("该批次已拨付，无法拒绝");
        }
        if (existPay != null && ReimburseConstants.PAY_STATUS_REJECTED.equals(existPay.getStatus())) {
            return Result.fail("该批次已拒绝拨付");
        }

        // 校验批次状态：仅已申报可拒绝
        Batch batch = batchService.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }
        if (ReimburseConstants.BATCH_STATUS_COMPLETED.equals(batch.getStatus())) {
            return Result.fail("该批次已完成，无法拒绝");
        }
        if (ReimburseConstants.BATCH_STATUS_PAY_REJECTED.equals(batch.getStatus())) {
            return Result.fail("该批次已被拒绝拨付");
        }
        if (!ReimburseConstants.BATCH_STATUS_DECLARED.equals(batch.getStatus())) {
            return Result.fail("仅已申报状态的批次可拒绝拨付");
        }

        Pay pay = existPay == null ? new Pay() : existPay;
        if (existPay == null) {
            pay.setBatchId(batchId);
            pay.setHospitalId(batch.getHospitalId());
        }
        pay.setStatus(ReimburseConstants.PAY_STATUS_REJECTED);
        pay.setAmount(batch.getTotalAmt());
        pay.setRejectReason(reason);
        pay.setPayTime(LocalDateTime.now());
        if (existPay == null) {
            this.save(pay);
        } else {
            this.updateById(pay);
        }

        batch.setStatus(ReimburseConstants.BATCH_STATUS_PAY_REJECTED);
        batchService.updateById(batch);

        log.info("拒绝拨付，批次 ID: {}, 理由: {}", batchId, reason);
        return Result.ok(pay);
    }
}
