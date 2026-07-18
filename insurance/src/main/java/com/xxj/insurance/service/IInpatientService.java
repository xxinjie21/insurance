package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Inpatient;

import java.math.BigDecimal;

public interface IInpatientService extends IService<Inpatient> {
    /** 入院登记 */
    Result admit(Long visitId, Long userId, Long hospitalId, String bedNo);
    /** 缴纳押金 */
    Result deposit(Long inpatientId, BigDecimal amount, String remark);
    /** 出院结算 */
    Result discharge(Long inpatientId);
    /** 住院列表 */
    Result hospitalList(Long hospitalId, PageDTO pageDTO);
    /** 患者本人住院列表 */
    Result myList(Long userId, PageDTO pageDTO);
}
