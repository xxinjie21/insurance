package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.RemoteMedicalFiling;

public interface IRemoteFilingService extends IService<RemoteMedicalFiling> {
    /** 异地就医备案 */
    Result file(Long userId, String insuredCity, String treatmentCity, Long treatmentHospitalId);
    /** 取消备案 */
    Result cancel(Long filingId);
    /** 查询用户有效备案 */
    Result myFilings(Long userId);
}
