package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Prescription;

public interface IPrescriptionService extends IService<Prescription> {
    /** 医生开方 */
    Result prescribe(Long visitId, Long doctorId);
    /** 药师审方（通过） */
    Result approve(Long prescriptionId);
    /** 药师驳回 */
    Result reject(Long prescriptionId, Long pharmacistId, String reason);
    /** 处方列表 */
    Result listByVisit(Long visitId);
}
