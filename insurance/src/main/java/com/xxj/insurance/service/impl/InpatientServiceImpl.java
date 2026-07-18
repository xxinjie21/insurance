package com.xxj.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Inpatient;
import com.xxj.insurance.domain.po.InpatientDeposit;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.mapper.InpatientDepositMapper;
import com.xxj.insurance.mapper.InpatientMapper;
import com.xxj.insurance.service.IInpatientService;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 住院管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InpatientServiceImpl extends ServiceImpl<InpatientMapper, Inpatient> implements IInpatientService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InpatientDepositMapper depositMapper;
    private final IVisitService visitService;
    private final ISettleService settleService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Result admit(Long visitId, Long userId, Long hospitalId, String bedNo) {
        Visit visit = visitService.getById(visitId);
        if (visit == null) return Result.fail("就诊记录不存在");
        if (visit.getType() == null || visit.getType() != 2) return Result.fail("非住院类型就诊，无法入院");

        LambdaQueryWrapper<Inpatient> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Inpatient::getVisitId, visitId);
        if (this.count(existWrapper) > 0) return Result.fail("该就诊已入院");

        String inpatientNo = "IP" + DateUtil.format(new Date(), "yyyyMMdd") + String.format("%04d", SECURE_RANDOM.nextInt(10000));

        Inpatient inpatient = new Inpatient();
        inpatient.setVisitId(visitId);
        inpatient.setUserId(userId);
        inpatient.setHospitalId(hospitalId);
        inpatient.setInpatientNo(inpatientNo);
        inpatient.setBedNo(bedNo);
        inpatient.setAdmissionTime(LocalDateTime.now());
        inpatient.setDepositTotal(BigDecimal.ZERO);
        inpatient.setTotalFee(BigDecimal.ZERO);
        inpatient.setStatus(0);
        inpatient.setCreateTime(LocalDateTime.now());
        save(inpatient);

        log.info("入院登记成功，inpatientNo:{}, visitId:{}", inpatientNo, visitId);
        return Result.ok(inpatient);
    }

    @Override
    public Result deposit(Long inpatientId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return Result.fail("押金金额必须大于0");
        Inpatient inpatient = getById(inpatientId);
        if (inpatient == null) return Result.fail("住院记录不存在");
        if (inpatient.getStatus() != 0) return Result.fail("非住院中状态，无法缴纳押金");

        InpatientDeposit deposit = new InpatientDeposit();
        deposit.setInpatientId(inpatientId);
        deposit.setAmount(amount);
        deposit.setType(1);
        deposit.setRemark(remark);
        deposit.setCreateTime(LocalDateTime.now());
        depositMapper.insert(deposit);

        inpatient.setDepositTotal(inpatient.getDepositTotal().add(amount));
        updateById(inpatient);

        log.info("押金缴纳成功，inpatientId:{}, amount:{}", inpatientId, amount);
        return Result.ok("押金缴纳成功");
    }

    @Override
    public Result discharge(Long inpatientId) {
        Inpatient inpatient = getById(inpatientId);
        if (inpatient == null) return Result.fail("住院记录不存在");
        if (inpatient.getStatus() != 0) return Result.fail("非住院中状态");

        // 事务内执行：汇总费用 → 调用结算 → 更新状态
        Result result = transactionTemplate.execute(status -> {
            // 汇总就诊下所有费用
            Visit visit = visitService.getById(inpatient.getVisitId());
            if (visit == null) {
                status.setRollbackOnly();
                return Result.fail("关联就诊记录不存在");
            }

            // 调用结算接口（复用现有结算逻辑）
            Result settleResult = settleService.calculate(inpatient.getVisitId());
            if (!settleResult.getSuccess()) {
                status.setRollbackOnly();
                return settleResult;
            }

            // 更新住院状态
            inpatient.setStatus(1);
            inpatient.setDischargeTime(LocalDateTime.now());
            updateById(inpatient);

            // 更新就诊状态
            visit.setStatus(1);
            visitService.updateById(visit);

            log.info("出院结算完成，inpatientId:{}, visitId:{}", inpatientId, inpatient.getVisitId());
            return Result.ok("出院结算完成");
        });

        return result != null ? result : Result.fail("操作失败，请重试");
    }

    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO) {
        pageDTO = (pageDTO != null && pageDTO.getPageNum() != null) ? pageDTO : new PageDTO(1, 10);
        Page<Inpatient> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<Inpatient> wrapper = new LambdaQueryWrapper<Inpatient>()
                .eq(Inpatient::getHospitalId, hospitalId)
                .orderByDesc(Inpatient::getCreateTime);
        return Result.ok(page(page, wrapper).getRecords(), page.getTotal());
    }

    @Override
    public Result myList(Long userId, PageDTO pageDTO) {
        pageDTO = (pageDTO != null && pageDTO.getPageNum() != null) ? pageDTO : new PageDTO(1, 10);
        Page<Inpatient> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<Inpatient> wrapper = new LambdaQueryWrapper<Inpatient>()
                .eq(Inpatient::getUserId, userId)
                .orderByDesc(Inpatient::getCreateTime);
        return Result.ok(page(page, wrapper).getRecords(), page.getTotal());
    }
}
