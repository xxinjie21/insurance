package com.xxj.insurance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Registration;

public interface IRegistrationService extends IService<Registration> {
    Result register(String idCard, Long hospitalId, String dept, String doctorName, Integer regType);
    Result myList(PageDTO pageDTO, Long hospitalId);
    Result hospitalList(Long hospitalId, PageDTO pageDTO);
}
