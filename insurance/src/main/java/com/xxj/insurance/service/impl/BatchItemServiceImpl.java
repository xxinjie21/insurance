package com.xxj.insurance.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.BatchItem;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.BatchItemVO;
import com.xxj.insurance.mapper.BatchItemMapper;
import com.xxj.insurance.mapper.BatchMapper;
import com.xxj.insurance.mapper.VisitMapper;
import com.xxj.insurance.service.IBatchItemService;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 申报明细表 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchItemServiceImpl extends ServiceImpl<BatchItemMapper, BatchItem> implements IBatchItemService {

    private final BatchMapper batchMapper;
    private final ISettleService settleService;
    private final IUserService userService;
    private final VisitMapper visitMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 医保局查询申报明细列表
     * 支持按批次ID和医院ID查询
     */
    // 医保局查询申报明细列表，支持按批次ID或医院ID筛选
    @Override
    public Result medicalList(Long batchId, Long hospitalId) {
        // 至少需要一个查询条件
        if (batchId == null && hospitalId == null) {
            return Result.fail("批次 ID 或医院 ID 至少需要一个");
        }

        // 如果指定了 batchId，直接查询该批次下的明细
        if (batchId != null) {
            LambdaQueryWrapper<BatchItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BatchItem::getBatchId, batchId);
            List<BatchItem> items = list(wrapper);
            return Result.ok(enrichBatchItemVOList(items));
        }

        // 按医院ID查询：先查询该院所有批次，再查这些批次的明细
        LambdaQueryWrapper<Batch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(Batch::getHospitalId, hospitalId);
        batchWrapper.orderByDesc(Batch::getCreateTime);

        List<Batch> batches = batchMapper.selectList(batchWrapper);
        if (batches.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 收集所有批次ID，查询对应的明细
        Set<Long> batchIds = new HashSet<>();
        for (Batch batch : batches) {
            batchIds.add(batch.getId());
        }
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(BatchItem::getBatchId, batchIds);
        List<BatchItem> items = list(itemWrapper);

        if (items.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        List<BatchItemVO> voList = enrichBatchItemVOList(items);

        // 补充批次信息到 VO 中
        Map<Long, Batch> batchMap = new HashMap<>();
        for (Batch batch : batches) {
            batchMap.put(batch.getId(), batch);
        }
        for (BatchItemVO vo : voList) {
            Batch batch = batchMap.get(vo.getBatchId());
            if (batch != null) {
                vo.setBatchCreateTime(batch.getCreateTime());
                vo.setBatchNo(batch.getBatchNo());
                vo.setBatchStatus(batch.getStatus());
            }
        }

        return Result.ok(voList);
    }

    /**
     * 医院查询本院申报明细列表
     * 支持按批次ID和创建时间范围查询
     */
    // 医院查询本院申报明细列表，支持批次ID和时间范围筛选
    @Override
    public Result hospitalList(Long hospitalId, Long batchId, LocalDateTime startTime, LocalDateTime endTime) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        // 查询该院所有批次（用于时间过滤）
        LambdaQueryWrapper<Batch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(Batch::getHospitalId, hospitalId);
        if (batchId != null) {
            batchWrapper.eq(Batch::getId, batchId);
        }
        if (startTime != null) {
            batchWrapper.ge(Batch::getCreateTime, startTime);
        }
        if (endTime != null) {
            batchWrapper.le(Batch::getCreateTime, endTime);
        }
        batchWrapper.orderByDesc(Batch::getCreateTime);

        List<Batch> batches = batchMapper.selectList(batchWrapper);
        if (batches.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 收集所有批次ID，查询对应的明细
        Set<Long> batchIds = new HashSet<>();
        for (Batch batch : batches) {
            batchIds.add(batch.getId());
        }
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(BatchItem::getBatchId, batchIds);
        List<BatchItem> items = list(itemWrapper);

        if (items.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        List<BatchItemVO> voList = enrichBatchItemVOList(items);

        // 补充批次创建时间到 VO 中（通过 batchId 关联）
        Map<Long, Batch> batchMap = new HashMap<>();
        for (Batch batch : batches) {
            batchMap.put(batch.getId(), batch);
        }
        for (BatchItemVO vo : voList) {
            Batch batch = batchMap.get(vo.getBatchId());
            if (batch != null) {
                vo.setBatchCreateTime(batch.getCreateTime());
                vo.setBatchNo(batch.getBatchNo());
                vo.setBatchStatus(batch.getStatus());
            }
        }

        return Result.ok(voList);
    }

    // 按批次分页查询申报明细，含权限校验
    @Override
    public Result listByBatchPage(Long batchId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        Batch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        Result permissionCheck = verifyBatchViewPermission(batch);
        if (permissionCheck != null) {
            return permissionCheck;
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<BatchItem> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<BatchItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchItem::getBatchId, batchId);
        if (startTime != null) {
            wrapper.ge(BatchItem::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(BatchItem::getCreateTime, endTime);
        }
        wrapper.orderByDesc(BatchItem::getCreateTime);

        Page<BatchItem> resultPage = page(page, wrapper);
        List<BatchItemVO> voList = enrichBatchItemVOList(resultPage.getRecords());

        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), resultPage.getTotal(), voList));
    }

    // 根据结算单ID查询所属批次信息
    @Override
    public Result getBySettleId(Long settleId) {
        if (settleId == null) {
            return Result.fail("结算单 ID 不能为空");
        }

        LambdaQueryWrapper<BatchItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchItem::getSettleId, settleId);
        BatchItem item = getOne(wrapper);

        if (item == null) {
            return Result.ok(null);
        }

        Batch batch = batchMapper.selectById(item.getBatchId());
        List<BatchItemVO> voList = enrichBatchItemVOList(Collections.singletonList(item));
        BatchItemVO vo = voList.isEmpty() ? null : voList.get(0);

        if (vo != null && batch != null) {
            vo.setBatchNo(batch.getBatchNo());
            vo.setBatchStatus(batch.getStatus());
            vo.setBatchCreateTime(batch.getCreateTime());
        }

        return Result.ok(vo);
    }

    /**
     * 校验当前用户是否有权查看该批次（与批次详情一致）
     */
    private Result verifyBatchViewPermission(Batch batch) {
        String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
        boolean isAdmin = roleStr != null && String.valueOf(Role.ADMIN.getCode()).equals(roleStr);
        boolean isMedical = roleStr != null && String.valueOf(Role.MEDICAL.getCode()).equals(roleStr);
        Long currentHospitalId = UserHolder.getHospitalId();
        if (!isAdmin && !isMedical) {
            if (currentHospitalId == null) {
                return Result.fail("无权限查询该批次");
            }
            if (!currentHospitalId.equals(batch.getHospitalId())) {
                return Result.fail("只能查询本院批次");
            }
        }
        return null;
    }

    /**
     * 批量查询并组装 BatchItemVO（含结算单信息、患者姓名）
     */
    private List<BatchItemVO> enrichBatchItemVOList(List<BatchItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询结算单 — Redis 缓存
        Set<Long> settleIds = items.stream()
                .map(BatchItem::getSettleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Settle> settleMap = new HashMap<>();
        for (Long id : settleIds) {
            String key = "cache:settle:" + id;
            String json = redisTemplate.opsForValue().get(key);
            Settle settle = null;
            if (json != null) {
                try {
                    settle = JSON.parseObject(json, Settle.class);
                } catch (Exception e) {
                    log.warn("缓存解析失败 key:{}", key, e);
                }
            }
            if (settle == null) {
                settle = settleService.getById(id);
                if (settle != null) {
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(settle), 30, TimeUnit.MINUTES);
                }
            }
            if (settle != null) {
                settleMap.put(id, settle);
            }
        }

        // 批量查询就诊记录 — Redis 缓存
        Set<Long> visitIds = settleMap.values().stream()
                .map(Settle::getVisitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Visit> visitMap = new HashMap<>();
        if (!visitIds.isEmpty()) {
            for (Long id : visitIds) {
                String key = "cache:visit:" + id;
                String json = redisTemplate.opsForValue().get(key);
                Visit visit = null;
                if (json != null) {
                    try {
                        visit = JSON.parseObject(json, Visit.class);
                    } catch (Exception e) {
                        log.warn("缓存解析失败 key:{}", key, e);
                    }
                }
                if (visit == null) {
                    visit = visitMapper.selectById(id);
                    if (visit != null) {
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(visit), 30, TimeUnit.MINUTES);
                    }
                }
                if (visit != null) {
                    visitMap.put(id, visit);
                }
            }
        }

        // 批量查询患者 — Redis 缓存
        Set<Long> userIds = visitMap.values().stream()
                .map(Visit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Long id : userIds) {
                String key = "cache:user:" + id;
                String json = redisTemplate.opsForValue().get(key);
                User user = null;
                if (json != null) {
                    try {
                        user = JSON.parseObject(json, User.class);
                    } catch (Exception e) {
                        log.warn("缓存解析失败 key:{}", key, e);
                    }
                }
                if (user == null) {
                    user = userService.getById(id);
                    if (user != null) {
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 30, TimeUnit.MINUTES);
                    }
                }
                if (user != null) {
                    userMap.put(id, user);
                }
            }
        }

        // 组装 VO
        List<BatchItemVO> voList = new ArrayList<>();
        for (BatchItem item : items) {
            BatchItemVO vo = new BatchItemVO();
            vo.setId(item.getId());
            vo.setBatchId(item.getBatchId());
            vo.setSettleId(item.getSettleId());
            vo.setAudit(item.getAudit());
            vo.setCreateTime(item.getCreateTime());

            Settle settle = settleMap.get(item.getSettleId());
            if (settle != null) {
                vo.setSettleTotal(settle.getTotal());
                vo.setSettleReimburse(settle.getReimburse());
                vo.setSettleSelfPay(settle.getSelfPay());
                vo.setSettleStatus(settle.getStatus());

                Visit visit = visitMap.get(settle.getVisitId());
                if (visit != null) {
                    User user = userMap.get(visit.getUserId());
                    if (user != null) {
                        vo.setPatientName(user.getName());
                        String idCard = user.getIdCard();
                        vo.setPatientIdCard(idCard);
                        vo.setIdCard(idCard);
                    }
                }
            }

            voList.add(vo);
        }

        return voList;
    }
}
