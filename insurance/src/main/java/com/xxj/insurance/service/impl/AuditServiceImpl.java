package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.domain.po.AuditRule;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.mapper.AuditRuleMapper;
import com.xxj.insurance.service.IAuditService;
import com.xxj.insurance.service.IFeeService;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 审核规则引擎实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    private final AuditRuleMapper auditRuleMapper;
    private final IFeeService feeService;
    private final IVisitService visitService;

    @Override
    public List<Map<String, Object>> auditSettle(Settle settle) {
        List<Map<String, Object>> findings = new ArrayList<>();
        if (settle == null) return findings;

        // 查就诊诊断
        Visit visit = visitService.getById(settle.getVisitId());
        if (visit == null) return findings;
        String diagnosis = visit.getDiagnosis();

        // 查费用清单
        List<Fee> fees = feeService.lambdaQuery().eq(Fee::getVisitId, settle.getVisitId()).list();
        if (fees.isEmpty()) return findings;

        // 收集费用名称列表
        Set<String> feeNames = new HashSet<>();
        for (Fee fee : fees) {
            if (StrUtil.isNotBlank(fee.getName())) feeNames.add(fee.getName());
        }

        // 加载所有启用的审核规则
        List<AuditRule> rules = auditRuleMapper.selectList(
                new LambdaQueryWrapper<AuditRule>().eq(AuditRule::getEnabled, 1));
        if (rules.isEmpty()) return findings;

        // 逐规则检查
        for (AuditRule rule : rules) {
            List<Map<String, Object>> result = checkRule(rule, diagnosis, feeNames, fees);
            if (result != null) findings.addAll(result);
        }

        return findings;
    }

    private List<Map<String, Object>> checkRule(AuditRule rule, String diagnosis,
                                                  Set<String> feeNames, List<Fee> fees) {
        List<Map<String, Object>> findings = new ArrayList<>();

        switch (rule.getRuleType()) {
            case "DIAG_DRUG_MATCH":
                // 诊断匹配：诊断名匹配paramKey时，检查费用是否在允许列表内
                if (diagnosis != null && diagnosis.contains(rule.getParamKey())) {
                    List<String> allowed = StrUtil.isNotBlank(rule.getParamValue())
                            ? Arrays.asList(rule.getParamValue().split(",")) : Collections.emptyList();
                    for (Fee fee : fees) {
                        if (StrUtil.isNotBlank(fee.getName()) && !matchesAny(fee.getName(), allowed)) {
                            // 费用名不在允许列表 → 诊断与费用不匹配
                            findings.add(buildFinding(rule, fee,
                                    "诊断[" + diagnosis + "]与费用[" + fee.getName() + "]可能不匹配"));
                        }
                    }
                }
                break;

            case "DUPLICATE_DRUG":
                // 重复用药：paramKey和paramValue两个药同时出现
                if (StrUtil.isNotBlank(rule.getParamKey()) && StrUtil.isNotBlank(rule.getParamValue())) {
                    List<String> conflicts = Arrays.asList(rule.getParamValue().split(","));
                    if (containsAny(feeNames, rule.getParamKey())) {
                        for (String conflict : conflicts) {
                            if (containsAny(feeNames, conflict)) {
                                findings.add(buildFindingNoFee(rule,
                                        "重复用药：[" + rule.getParamKey() + "]与[" + conflict + "]药理作用重叠"));
                            }
                        }
                    }
                }
                break;

            case "AGE_RESTRICT":
                // 年龄限制：费用含paramKey时检查（演示简化：记录预警）
                if (containsAny(feeNames, rule.getParamKey())) {
                    findings.add(buildFindingNoFee(rule,
                            "药品[" + rule.getParamKey() + "]限制患者年龄≥" + rule.getParamValue() + "岁"));
                }
                break;

            case "SEX_RESTRICT":
                if (containsAny(feeNames, rule.getParamKey())) {
                    findings.add(buildFindingNoFee(rule,
                            "药品[" + rule.getParamKey() + "]有性别使用限制：" + rule.getParamValue()));
                }
                break;
        }

        return findings;
    }

    private boolean matchesAny(String name, List<String> keywords) {
        for (String kw : keywords) {
            if (name.contains(kw.trim())) return true;
        }
        return false;
    }

    private boolean containsAny(Set<String> names, String keyword) {
        for (String name : names) {
            if (name.contains(keyword.trim())) return true;
        }
        return false;
    }

    private Map<String, Object> buildFinding(AuditRule rule, Fee fee, String description) {
        Map<String, Object> finding = new HashMap<>();
        finding.put("ruleDescription", description);
        finding.put("severity", rule.getSeverity());
        finding.put("feeId", fee.getId());
        finding.put("feeName", fee.getName());
        // 扣款类：建议调减该费用金额的50%
        BigDecimal suggestDeduct = BigDecimal.ZERO;
        if (rule.getSeverity() != null && rule.getSeverity() == 2 && fee.getTotal() != null) {
            suggestDeduct = fee.getTotal().multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        finding.put("suggestDeductAmount", suggestDeduct);
        return finding;
    }

    private Map<String, Object> buildFindingNoFee(AuditRule rule, String description) {
        Map<String, Object> finding = new HashMap<>();
        finding.put("ruleDescription", description);
        finding.put("severity", rule.getSeverity());
        finding.put("feeId", null);
        finding.put("feeName", null);
        finding.put("suggestDeductAmount", BigDecimal.ZERO);
        return finding;
    }
}
