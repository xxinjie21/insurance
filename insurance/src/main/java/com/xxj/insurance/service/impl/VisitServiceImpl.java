package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.VisitAddDTO;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.VisitVO;
import com.xxj.insurance.mapper.VisitMapper;
import com.xxj.insurance.mapper.FeeMapper;
import com.xxj.insurance.mapper.SettleMapper;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IUserService;
import com.xxj.insurance.service.IVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 就诊 Service 实现类
 * 修复记录：
 * - N+1 查询：convertToVoPage 改为批量查询用户和医院
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl extends ServiceImpl<VisitMapper, Visit> implements IVisitService {

    private final IHospitalService hospitalService;
    private final IUserService userService;
    private final FeeMapper feeMapper;
    private final SettleMapper settleMapper;
    private final StringRedisTemplate redisTemplate;

    // 新增就诊记录（验证患者 + 自动绑定医院）
    @Override
    public Result add(VisitAddDTO dto) {
        if (dto == null) {
            return Result.fail("就诊信息不能为空");
        }
        if (StrUtil.isBlank(dto.getIdCard())) {
            return Result.fail("身份证号不能为空");
        }
        if (dto.getHospitalId() == null) {
            return Result.fail("医院 ID 不能为空");
        }
        if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
            return Result.fail("就诊类型无效");
        }
        if (StrUtil.isBlank(dto.getDiagnosis())) {
            return Result.fail("诊断结果不能为空");
        }

        Hospital hospital = hospitalService.getById(dto.getHospitalId());
        if (hospital == null) {
            return Result.fail("医院不存在");
        }

        // 通过身份证号查找患者（唯一标识）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getIdCard, dto.getIdCard());
        User user = userService.getOne(userWrapper);
        if (user == null) {
            return Result.fail("该身份证号对应的患者不存在，请先注册患者账号");
        }
        if (user.getRole() == null || !Role.PATIENT.getCode().equals(user.getRole())) {
            return Result.fail("该身份证号对应的用户不是患者角色");
        }

        Visit visit = new Visit();
        visit.setUserId(user.getId());
        visit.setHospitalId(dto.getHospitalId());
        visit.setType(dto.getType());
        visit.setDiagnosis(dto.getDiagnosis());
        visit.setStatus(ReimburseConstants.VISIT_STATUS_PENDING);
        visit.setCreateTime(LocalDateTime.now());
        save(visit);

        VisitVO visitVO = new VisitVO();
        BeanUtils.copyProperties(visit, visitVO);
        visitVO.setHospitalName(hospital.getName());
        visitVO.setUserName(user.getName());

        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visit.getId();
        redisTemplate.delete(cacheKey);

        // 异步将患者绑定到当前医院（自动加入本院患者列表）
        userService.asyncBindHospital(user.getId(), dto.getHospitalId());

        return Result.ok(visitVO);
    }

    // 患者侧：查看就诊记录列表
    @Override
    public Result myList(PageDTO pageDTO, Long hospitalId, LocalDateTime startTime, LocalDateTime endTime) {
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<Visit> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }

        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visit::getUserId, userId);
        if (hospitalId != null) {
            wrapper.eq(Visit::getHospitalId, hospitalId);
        }
        if (startTime != null) {
            wrapper.ge(Visit::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Visit::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Visit::getCreateTime);

        Page<Visit> visitPage = page(page, wrapper);

        // 批量查询转换（修复 N+1）
        Page<VisitVO> voPage = convertToVoPageBatch(visitPage);

        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), voPage.getTotal(), voPage.getRecords()));
    }

    // 医院侧：就诊列表，支持按患者姓名 / ID 过滤
    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO, String patientName, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<Visit> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visit::getHospitalId, hospitalId);

        if (userId != null) {
            wrapper.eq(Visit::getUserId, userId);
        } else if (StrUtil.isNotBlank(patientName)) {
            List<Long> userIds = userService.lambdaQuery()
                    .like(User::getName, patientName)
                    .list()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
            }
            wrapper.in(Visit::getUserId, userIds);
        }
        if (startTime != null) {
            wrapper.ge(Visit::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Visit::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Visit::getCreateTime);
        Page<Visit> visitPage = page(page, wrapper);

        // 批量查询转换（修复 N+1）
        Page<VisitVO> voPage = convertToVoPageBatch(visitPage);

        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), voPage.getTotal(), voPage.getRecords()));
    }

    // 查看就诊详情
    @Override
    public Result getVisitById(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        Visit visit = getWithCache(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        VisitVO vo = convertToVoSingle(visit);

        return Result.ok(vo);
    }

    // 查就诊（Redis 缓存）
    public Visit getWithCache(Long visitId) {
        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visitId;

        String cacheJson = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheJson)) {
            try {
                return JSON.parseObject(cacheJson, Visit.class);
            } catch (Exception e) {
                log.warn("缓存解析失败 key:{}", cacheKey, e);
            }
        }

        Visit visit = getById(visitId);

        if (visit != null) {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(visit),
                RedisConstants.CACHE_VISIT_TTL, TimeUnit.MINUTES);
        }

        return visit;
    }

    // 单条记录转 VO（用于详情查询）
    private VisitVO convertToVoSingle(Visit visit) {
        VisitVO vo = new VisitVO();
        BeanUtils.copyProperties(visit, vo);

        if (visit.getUserId() != null) {
            User user = userService.getById(visit.getUserId());
            fillPatientIdCard(vo, user);
        }

        if (visit.getHospitalId() != null) {
            Hospital hospital = hospitalService.getById(visit.getHospitalId());
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }
        }

        return vo;
    }

    // 填充患者姓名和身份证号
    private void fillPatientIdCard(VisitVO vo, User user) {
        if (vo == null || user == null) {
            return;
        }
        vo.setUserName(user.getName());
        String idCard = user.getIdCard();
        vo.setPatientIdCard(idCard);
        vo.setIdCard(idCard);
    }

    // 批量转换 VO：一次查用户 + 一次查医院，避免 N+1
    private Page<VisitVO> convertToVoPageBatch(Page<Visit> visitPage) {
        Page<VisitVO> voPage = new Page<>(
            visitPage.getCurrent(),
            visitPage.getSize(),
            visitPage.getTotal()
        );

        List<Visit> records = visitPage.getRecords();
        if (records.isEmpty()) {
            return voPage;
        }

        // 批量查询用户
        Set<Long> userIds = records.stream()
                .map(Visit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        // 批量查询医院
        Set<Long> hospitalIds = records.stream()
                .map(Visit::getHospitalId)
                .collect(Collectors.toSet());
        Map<Long, Hospital> hospitalMap = hospitalService.listByIds(hospitalIds).stream()
                .collect(Collectors.toMap(Hospital::getId, Function.identity(), (a, b) -> a));

        // 转换
        List<VisitVO> voList = records.stream().map(visit -> {
            VisitVO vo = new VisitVO();
            BeanUtils.copyProperties(visit, vo);

            fillPatientIdCard(vo, userMap.get(visit.getUserId()));

            Hospital hospital = hospitalMap.get(visit.getHospitalId());
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    // 删除就诊记录（已结算 / 有费用 / 有结算时不允许）
    @Override
    public Result delete(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        Visit visit = getById(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        // 已结算的就诊记录不允许删除
        if (ReimburseConstants.VISIT_STATUS_SETTLED.equals(visit.getStatus())) {
            return Result.fail("已结算的就诊记录不允许删除");
        }

        // 检查是否有关联的费用明细
        Long feeCount = feeMapper.selectCount(
            new LambdaQueryWrapper<Fee>().eq(Fee::getVisitId, visitId));
        if (feeCount != null && feeCount > 0) {
            return Result.fail("该就诊记录存在费用明细，请先删除费用明细");
        }

        // 检查是否有关联的结算记录
        Long settleCount = settleMapper.selectCount(
            new LambdaQueryWrapper<Settle>().eq(Settle::getVisitId, visitId));
        if (settleCount != null && settleCount > 0) {
            return Result.fail("该就诊记录存在结算记录，无法删除");
        }

        // 删除就诊记录
        removeById(visitId);

        // 同步清除 Redis 缓存，避免脏数据
        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visitId;
        redisTemplate.delete(cacheKey);

        return Result.ok("删除成功");
    }
}
