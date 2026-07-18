package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Doctor;
import com.xxj.insurance.mapper.DoctorMapper;
import com.xxj.insurance.service.IDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements IDoctorService {

    @Override
    public Result listByHospital(Long hospitalId, PageDTO pageDTO, String keyword) {
        pageDTO = (pageDTO != null && pageDTO.getPageNum() != null) ? pageDTO : new PageDTO(1, 20);
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getHospitalId, hospitalId)
                .eq(Doctor::getStatus, 1);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Doctor::getName, keyword).or().like(Doctor::getDept, keyword));
        }
        wrapper.orderByAsc(Doctor::getDept);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Doctor> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        page(page, wrapper);
        return Result.ok(page.getRecords(), page.getTotal());
    }
}
