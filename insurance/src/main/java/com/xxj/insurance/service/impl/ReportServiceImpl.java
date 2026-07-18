package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.*;
import com.xxj.insurance.mapper.*;
import com.xxj.insurance.service.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private final SettleMapper settleMapper;
    private final BatchMapper batchMapper;
    private final FeeMapper feeMapper;
    private final VisitMapper visitMapper;
    private final HospitalMapper hospitalMapper;
    private final UserMapper userMapper;

    @Override
    public Result fundReport(Integer year, Integer month) {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, BigDecimal> hospitalAmounts = new LinkedHashMap<>();
        BigDecimal totalPooling = BigDecimal.ZERO;
        BigDecimal totalCatastrophic = BigDecimal.ZERO;
        BigDecimal totalAssistance = BigDecimal.ZERO;

        List<Settle> settles = querySettles(year, month);
        for (Settle s : settles) {
            Hospital h = hospitalMapper.selectById(s.getHospitalId());
            String name = h != null ? h.getName() : "未知";
            BigDecimal p = safe(s.getPoolingPay()).add(safe(s.getCatastrophicPay())).add(safe(s.getAssistancePay()));
            hospitalAmounts.merge(name, p, BigDecimal::add);
            totalPooling = totalPooling.add(safe(s.getPoolingPay()));
            totalCatastrophic = totalCatastrophic.add(safe(s.getCatastrophicPay()));
            totalAssistance = totalAssistance.add(safe(s.getAssistancePay()));
        }

        long batchCount = batchMapper.selectCount(new LambdaQueryWrapper<Batch>()
                .eq(Batch::getStatus, 2));
        report.put("year", year);
        report.put("month", month);
        report.put("totalFundPay", totalPooling.add(totalCatastrophic).add(totalAssistance));
        report.put("totalPooling", totalPooling);
        report.put("totalCatastrophic", totalCatastrophic);
        report.put("totalAssistance", totalAssistance);
        report.put("settleCount", settles.size());
        report.put("completedBatchCount", batchCount);
        report.put("hospitalDetails", hospitalAmounts);
        return Result.ok(report);
    }

    @Override
    public Result feeCompositionReport(Integer year, Integer month) {
        Map<String, Object> report = new LinkedHashMap<>();
        List<Fee> fees = queryFees(year, month);

        BigDecimal drugTotal = BigDecimal.ZERO;
        BigDecimal checkTotal = BigDecimal.ZERO;
        BigDecimal treatmentTotal = BigDecimal.ZERO;
        BigDecimal consumableTotal = BigDecimal.ZERO;

        for (Fee f : fees) {
            BigDecimal t = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            if (anyMatch(f.getName(), "片", "胶囊", "颗粒", "注射", "口服", "针")) drugTotal = drugTotal.add(t);
            else if (anyMatch(f.getName(), "检查", "镜", "超声", "X线", "CT", "MRI", "心电图", "B超", "血常规", "尿常规", "肝功能", "肾功能")) checkTotal = checkTotal.add(t);
            else if (anyMatch(f.getName(), "手术", "清创", "缝合", "护理", "诊查")) treatmentTotal = treatmentTotal.add(t);
            else consumableTotal = consumableTotal.add(t);
        }

        report.put("year", year);
        report.put("month", month);
        report.put("totalFeeCount", fees.size());
        report.put("drugFee", drugTotal);
        report.put("checkFee", checkTotal);
        report.put("treatmentFee", treatmentTotal);
        report.put("consumableFee", consumableTotal);
        return Result.ok(report);
    }

    @Override
    public Result visitStatsReport(Integer year, Integer month) {
        Map<String, Object> report = new LinkedHashMap<>();
        List<Settle> settles = querySettles(year, month);

        long outpatientCount = 0, inpatientCount = 0;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalReimburse = BigDecimal.ZERO;

        for (Settle s : settles) {
            Visit v = visitMapper.selectById(s.getVisitId());
            if (v != null) {
                if (v.getType() != null && v.getType() == 2) inpatientCount++;
                else outpatientCount++;
            }
            totalFee = totalFee.add(s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO);
            BigDecimal reimb = safe(s.getPoolingPay()).add(safe(s.getCatastrophicPay())).add(safe(s.getAssistancePay()));
            totalReimburse = totalReimburse.add(reimb);
        }

        long patientCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 1));
        report.put("year", year);
        report.put("month", month);
        report.put("outpatientCount", outpatientCount);
        report.put("inpatientCount", inpatientCount);
        report.put("totalVisitCount", outpatientCount + inpatientCount);
        report.put("totalFee", totalFee);
        report.put("totalReimburse", totalReimburse);
        report.put("avgFeePerVisit", settles.isEmpty() ? 0 :
                totalFee.divide(BigDecimal.valueOf(settles.size()), 2, java.math.RoundingMode.HALF_UP));
        report.put("reimburseRatio", totalFee.compareTo(BigDecimal.ZERO) == 0 ? "0%" :
                totalReimburse.divide(totalFee, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1) + "%");
        report.put("registeredPatientCount", patientCount);
        return Result.ok(report);
    }

    private List<Settle> querySettles(Integer year, Integer month) {
        String start = String.format("%04d-%02d-01 00:00:00", year, month != null ? month : 1);
        String end = month != null
                ? String.format("%04d-%02d-01 00:00:00", year, month == 12 ? year + 1 : year, month == 12 ? 1 : month + 1)
                : String.format("%04d-12-31 23:59:59", year);
        return settleMapper.selectList(new LambdaQueryWrapper<Settle>()
                .ge(Settle::getCreateTime, start).lt(Settle::getCreateTime, end));
    }

    private List<Fee> queryFees(Integer year, Integer month) {
        String start = String.format("%04d-%02d-01 00:00:00", year, month != null ? month : 1);
        String end = month != null
                ? String.format("%04d-%02d-01 00:00:00", year, month == 12 ? year + 1 : year, month == 12 ? 1 : month + 1)
                : String.format("%04d-12-31 23:59:59", year);
        return feeMapper.selectList(new LambdaQueryWrapper<Fee>()
                .ge(Fee::getCreateTime, start).lt(Fee::getCreateTime, end));
    }

    private BigDecimal safe(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private boolean anyMatch(String name, String... keywords) {
        if (name == null || keywords == null) return false;
        for (String kw : keywords) if (name.contains(kw)) return true;
        return false;
    }
}
