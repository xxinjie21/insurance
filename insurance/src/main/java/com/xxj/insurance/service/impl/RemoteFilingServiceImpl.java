package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.RemoteMedicalFiling;
import com.xxj.insurance.mapper.RemoteMedicalFilingMapper;
import com.xxj.insurance.service.IRemoteFilingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteFilingServiceImpl extends ServiceImpl<RemoteMedicalFilingMapper, RemoteMedicalFiling> implements IRemoteFilingService {

    @Override
    public Result file(Long userId, String insuredCity, String treatmentCity, Long treatmentHospitalId) {
        if (StrUtil.isBlank(insuredCity) || StrUtil.isBlank(treatmentCity)) {
            return Result.fail("参保地和就医地不能为空");
        }
        RemoteMedicalFiling filing = new RemoteMedicalFiling();
        filing.setUserId(userId);
        filing.setInsuredCity(insuredCity);
        filing.setTreatmentCity(treatmentCity);
        filing.setTreatmentHospitalId(treatmentHospitalId);
        filing.setFilingStatus(1);
        filing.setStartDate(LocalDate.now());
        filing.setEndDate(LocalDate.now().plusYears(1));
        filing.setCreateTime(LocalDateTime.now());
        save(filing);
        log.info("异地备案成功，userId:{}, treatmentCity:{}", userId, treatmentCity);
        return Result.ok(filing);
    }

    @Override
    public Result cancel(Long filingId) {
        RemoteMedicalFiling filing = getById(filingId);
        if (filing == null) return Result.fail("备案不存在");
        filing.setFilingStatus(3);
        updateById(filing);
        return Result.ok("已取消");
    }

    @Override
    public Result myFilings(Long userId) {
        return Result.ok(lambdaQuery().eq(RemoteMedicalFiling::getUserId, userId)
                .eq(RemoteMedicalFiling::getFilingStatus, 1)
                .orderByDesc(RemoteMedicalFiling::getCreateTime).list());
    }
}
