package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Doctor;
import com.xxj.insurance.domain.po.Prescription;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.mapper.PrescriptionMapper;
import com.xxj.insurance.service.IDoctorService;
import com.xxj.insurance.service.IPrescriptionService;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements IPrescriptionService {

    private final IVisitService visitService;
    private final IDoctorService doctorService;

    @Override
    public Result prescribe(Long visitId, Long doctorId) {
        Visit visit = visitService.getById(visitId);
        if (visit == null) return Result.fail("就诊记录不存在");
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null || doctor.getStatus() == 0) return Result.fail("医生不存在或已停用");
        if (!doctor.getHospitalId().equals(visit.getHospitalId())) return Result.fail("医生不属于该就诊医院");

        Prescription rx = new Prescription();
        rx.setVisitId(visitId);
        rx.setDoctorId(doctorId);
        rx.setStatus(0); // 待审核
        rx.setCreateTime(LocalDateTime.now());
        save(rx);

        log.info("处方开具成功，rxId:{}, visitId:{}, doctorId:{}", rx.getId(), visitId, doctorId);
        return Result.ok(rx);
    }

    @Override
    public Result approve(Long prescriptionId) {
        Prescription rx = getById(prescriptionId);
        if (rx == null) return Result.fail("处方不存在");
        if (rx.getStatus() != 0) return Result.fail("仅待审核状态的处方可审核通过");
        rx.setStatus(1); // 审核通过
        updateById(rx);
        log.info("处方审核通过，rxId:{}", prescriptionId);
        return Result.ok("审核通过");
    }

    @Override
    public Result reject(Long prescriptionId, Long pharmacistId, String reason) {
        Prescription rx = getById(prescriptionId);
        if (rx == null) return Result.fail("处方不存在");
        if (rx.getStatus() != 0) return Result.fail("仅待审核状态的处方可驳回");
        rx.setStatus(2);
        rx.setPharmacistId(pharmacistId);
        rx.setRejectReason(reason);
        updateById(rx);
        log.info("处方驳回，rxId:{}, pharmacistId:{}, reason:{}", prescriptionId, pharmacistId, reason);
        return Result.ok("已驳回");
    }

    @Override
    public Result listByVisit(Long visitId) {
        return Result.ok(lambdaQuery().eq(Prescription::getVisitId, visitId)
                .orderByDesc(Prescription::getCreateTime).list());
    }
}
