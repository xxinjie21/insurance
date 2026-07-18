package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;

public interface IReportService {
    /** 基金收支报表（按医院/月度） */
    Result fundReport(Integer year, Integer month);
    /** 费用构成分析 */
    Result feeCompositionReport(Integer year, Integer month);
    /** 就诊统计报表 */
    Result visitStatsReport(Integer year, Integer month);
}
