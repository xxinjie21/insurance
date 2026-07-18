package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.Registration;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.mapper.RegistrationMapper;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IRegistrationService;
import com.xxj.insurance.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门诊挂号 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements IRegistrationService {

    private final IUserService userService;
    private final IHospitalService hospitalService;

    @Override
    public Result register(String idCard, Long hospitalId, String dept, String doctorName, Integer regType) {
        User user = userService.lambdaQuery().eq(User::getIdCard, idCard).one();
        if (user == null || !Role.PATIENT.getCode().equals(user.getRole())) {
            return Result.fail("患者不存在");
        }
        Hospital hospital = hospitalService.getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }
        if (regType == null || (regType != 1 && regType != 2)) {
            return Result.fail("挂号类型无效");
        }
        BigDecimal regFee = regType == 2 ? new BigDecimal("30.00") : new BigDecimal("15.00");

        Registration reg = new Registration();
        reg.setUserId(user.getId());
        reg.setHospitalId(hospitalId);
        reg.setDept(dept);
        reg.setDoctorName(doctorName);
        reg.setRegType(regType);
        reg.setRegFee(regFee);
        reg.setStatus(0);
        reg.setCreateTime(LocalDateTime.now());
        save(reg);

        log.info("挂号成功，regId:{}, userId:{}, hospitalId:{}", reg.getId(), user.getId(), hospitalId);
        return Result.ok(reg);
    }

    @Override
    public Result myList(PageDTO pageDTO, Long hospitalId) {
        pageDTO = (pageDTO != null && pageDTO.getPageNum() != null) ? pageDTO : new PageDTO(1, 10);
        Long userId = UserHolder.getUserId();
        Page<Registration> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId);
        if (hospitalId != null) wrapper.eq(Registration::getHospitalId, hospitalId);
        wrapper.orderByDesc(Registration::getCreateTime);
        Page<Registration> result = page(page, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO) {
        pageDTO = (pageDTO != null && pageDTO.getPageNum() != null) ? pageDTO : new PageDTO(1, 10);
        Page<Registration> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getHospitalId, hospitalId)
                .orderByDesc(Registration::getCreateTime);
        Page<Registration> result = page(page, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }
}
