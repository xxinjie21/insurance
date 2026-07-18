package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;

public interface IYearEndService {
    /** 年度结转：重置所有year_accumulate、个账计息 */
    Result rollover();
    /** 年度对账报表 */
    Result reconcileReport(Integer year);
}
