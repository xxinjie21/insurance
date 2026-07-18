package com.xxj.insurance.service;

import com.xxj.insurance.domain.po.Settle;

import java.util.List;
import java.util.Map;

/**
 * 审核规则引擎接口
 */
public interface IAuditService {

    /**
     * 审核一笔结算单，返回发现的问题列表
     * @return [{ruleDescription, severity(1预警/2扣款), suggestDeductAmount}]
     */
    List<Map<String, Object>> auditSettle(Settle settle);
}
