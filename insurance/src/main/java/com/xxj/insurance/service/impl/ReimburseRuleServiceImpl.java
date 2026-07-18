package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.domain.po.ReimburseRule;
import com.xxj.insurance.mapper.ReimburseRuleMapper;
import com.xxj.insurance.service.IReimburseRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 报销规则引擎实现
 *
 * @author xxj
 * @since 2026-07-18
 */
@Slf4j
@Service
public class ReimburseRuleServiceImpl extends ServiceImpl<ReimburseRuleMapper, ReimburseRule> implements IReimburseRuleService {

    /**
     * 查找匹配规则，无精确匹配时按同参保类型+同就诊类型最近等级回退
     */
    @Override
    public ReimburseRule findRule(Integer insuranceType, Integer hospitalLevel, Integer visitType) {
        LambdaQueryWrapper<ReimburseRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReimburseRule::getInsuranceType, insuranceType)
               .eq(ReimburseRule::getHospitalLevel, hospitalLevel)
               .eq(ReimburseRule::getVisitType, visitType);
        ReimburseRule rule = this.getOne(wrapper);
        if (rule != null) {
            return rule;
        }
        // 回退：同参保类型+同就诊类型下最近等级
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReimburseRule::getInsuranceType, insuranceType)
               .eq(ReimburseRule::getVisitType, visitType)
               .orderByDesc(ReimburseRule::getHospitalLevel);
        rule = this.getOne(wrapper);
        if (rule != null) {
            log.info("规则回退：insuranceType={} hospitalLevel={} visitType={} -> level={}",
                    insuranceType, hospitalLevel, visitType, rule.getHospitalLevel());
        }
        return rule;
    }
}
