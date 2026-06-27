package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;

public interface IDashboardService {

    Result getStats(Integer role);
}
