package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xxj.insurance.common.constants.AccountConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.*;
import com.xxj.insurance.mapper.*;
import com.xxj.insurance.service.IYearEndService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 年度管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YearEndServiceImpl implements IYearEndService {

    private final YearAccumulateMapper yearAccumulateMapper;
    private final SettleMapper settleMapper;
    private final BatchMapper batchMapper;
    private final UserMapper userMapper;
    private final HospitalMapper hospitalMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Result rollover() {
        int currentYear = LocalDateTime.now().getYear();
        Result result = transactionTemplate.execute(status -> {
            // 1. 年度累积累计：清除去年的起付线和统筹累计
            LambdaUpdateWrapper<YearAccumulate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(YearAccumulate::getYear, currentYear - 1)
                    .set(YearAccumulate::getDeductibleUsed, BigDecimal.ZERO)
                    .set(YearAccumulate::getPoolingTotal, BigDecimal.ZERO)
                    .setSql("year = year + 1");
            yearAccumulateMapper.update(null, updateWrapper);

            // 2. 个账计息：职工医保个人账户余额 × 年利率
            BigDecimal interestRate = new BigDecimal("0.015"); // 1.5%
            List<User> employeeUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getInsuranceType, 1)
                            .gt(User::getPersonalAccountBalance, BigDecimal.ZERO));
            for (User user : employeeUsers) {
                BigDecimal interest = user.getPersonalAccountBalance()
                        .multiply(interestRate).setScale(2, RoundingMode.HALF_UP);
                user.setPersonalAccountBalance(user.getPersonalAccountBalance().add(interest));
                userMapper.updateById(user);
            }
            log.info("年度结转完成，year:{}, 计息用户数:{}", currentYear, employeeUsers.size());
            return Result.ok("年度结转完成，计息用户" + employeeUsers.size() + "人");
        });
        return result != null ? result : Result.fail("结转失败");
    }

    @Override
    public Result reconcileReport(Integer year) {
        if (year == null) year = LocalDateTime.now().getYear();
        Map<String, Object> report = new HashMap<>();

        // 统筹支付总额（按医院聚合）
        List<Settle> settles = settleMapper.selectList(new LambdaQueryWrapper<Settle>()
                .ge(Settle::getCreateTime, year + "-01-01 00:00:00")
                .le(Settle::getCreateTime, year + "-12-31 23:59:59")
                .eq(Settle::getStatus, 3)); // 已拨付

        Map<String, BigDecimal> hospitalAmounts = new LinkedHashMap<>();
        BigDecimal totalPooling = BigDecimal.ZERO;
        for (Settle s : settles) {
            Hospital h = hospitalMapper.selectById(s.getHospitalId());
            String name = h != null ? h.getName() : "未知(" + s.getHospitalId() + ")";
            BigDecimal pooling = s.getPoolingPay() != null ? s.getPoolingPay() : BigDecimal.ZERO;
            hospitalAmounts.merge(name, pooling, BigDecimal::add);
            totalPooling = totalPooling.add(pooling);
        }

        // 批次统计
        long batchCount = batchMapper.selectCount(new LambdaQueryWrapper<Batch>()
                .ge(Batch::getCreateTime, year + "-01-01")
                .le(Batch::getCreateTime, year + "-12-31")
                .eq(Batch::getStatus, 2)); // 已完成

        report.put("year", year);
        report.put("totalPoolingPay", totalPooling);
        report.put("settleCount", settles.size());
        report.put("completedBatchCount", batchCount);
        report.put("hospitalDetails", hospitalAmounts);

        return Result.ok(report);
    }
}
