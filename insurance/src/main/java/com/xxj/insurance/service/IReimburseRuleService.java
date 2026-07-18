package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.domain.po.ReimburseRule;

/**
 * 报销规则服务接口
 *
 * @author xxj
 * @since 2026-07-18
 */
public interface IReimburseRuleService extends IService<ReimburseRule> {

    /**
     * 根据参保类型+医院等级+就诊类型查询匹配规则
     * 无精确匹配时回退到同参保类型同就诊类型的最低医院等级规则
     */
    ReimburseRule findRule(Integer insuranceType, Integer hospitalLevel, Integer visitType);
}
