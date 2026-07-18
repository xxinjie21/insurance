package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Doctor;

public interface IDoctorService extends IService<Doctor> {
    Result listByHospital(Long hospitalId, PageDTO pageDTO, String keyword);
}
