package com.xxj.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.BatchItem;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.Pay;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.BatchItemVO;
import com.xxj.insurance.domain.vo.BatchVO;
import com.xxj.insurance.mapper.BatchMapper;
import com.xxj.insurance.mapper.VisitMapper;
import com.xxj.insurance.service.IBatchItemService;
import com.xxj.insurance.service.IBatchService;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IPayService;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 批次管理 Service 实现类
 * 修复记录：
 * - 分布式锁 leaseTime: -1 改为 30 秒
 * - 幂等标识写入时机：移到事务提交成功后
 * - 魔术数字 "4" 改为 Role.ADMIN.getCode()
 * - 事务回滚 NPE 兜底
 * - SecureRandom 改为类级别 static final
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceImpl extends ServiceImpl<BatchMapper, Batch> implements IBatchService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // 复用，避免每次创建
    private static final long LOCK_LEASE_TIME = 30; // 锁持有时间 30 秒

    private final IBatchItemService batchItemService;
    private final ISettleService settleService;
    private final IUserService userService;
    private final IHospitalService hospitalService;
    private final VisitMapper visitMapper;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    @Lazy
    @Autowired
    private IPayService payService;

    // 从医院未申报结算单创建批次，同一医院可同时存在多个待申报批次
    @Override
    public Result createBatch(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        String lockKey = "lock:batch:create:" + hospitalId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 不使用幂等键：创建批次允许同一医院创建多个批次
            // 分布式锁已足够防止并发创建，幂等键会阻止正常的多批次创建
            Result result = transactionTemplate.execute(status -> executeCreateBatchWithTransaction(hospitalId));

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("创建批次被中断", e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 事务内：校验权限 -> 生成批次号 -> 写入数据库
    public Result executeCreateBatchWithTransaction(Long hospitalId) {
        // 权限校验：使用 Role 枚举
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能创建批次");
            }
        } else if (!currentHospitalId.equals(hospitalId)) {
            return Result.fail("只能为当前登录医院创建批次");
        }

        // 生成批次号（使用类级别 SecureRandom）
        String batchNo = DateUtil.format(new Date(), "yyyyMMddHHmmss")
                + String.format("%04d", hospitalId % 10000)
                + String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        Batch batch = new Batch();
        batch.setHospitalId(hospitalId);
        batch.setBatchNo(batchNo);
        batch.setSettleCnt(0);
        batch.setTotalAmt(BigDecimal.ZERO);
        batch.setStatus(ReimburseConstants.BATCH_STATUS_PENDING);
        batch.setCreateTime(LocalDateTime.now());

        this.save(batch);

        // 注意：幂等标记已移到事务提交后（外层 createBatch 方法中）
        log.info("创建批次成功，批次号：{}", batchNo);
        return Result.ok(batch);
    }

    // 添加结算单到批次，分布式锁 + 幂等键 + 事务三重防护
    @Override
    public Result addSettleToBatch(Long batchId, Long settleId) {
        if (batchId == null || settleId == null) {
            return Result.fail("批次 ID 和结算单 ID 不能为空");
        }

        String lockKey = "lock:batch:add:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:batch:item:" + settleId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            if (idempotentBucket.isExists()) {
                return Result.fail("该结算单已添加到批次，请勿重复添加");
            }

            Result result = transactionTemplate.execute(status -> executeAddSettleToBatchWithTransaction(batchId, settleId));

            // 事务提交成功后才写幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("添加结算单被中断", e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 事务内：校验批次/结算单状态 -> 写入批次明细 -> 更新批次统计和结算单状态
    public Result executeAddSettleToBatchWithTransaction(Long batchId, Long settleId) {
        String idempotentKey = "idempotent:batch:item:" + settleId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        if (idempotentBucket.isExists()) {
            return Result.fail("该结算单已添加到批次，请勿重复添加");
        }

        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        // 权限校验：使用 Role 枚举
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能操作批次");
            }
        } else if (!currentHospitalId.equals(batch.getHospitalId())) {
            return Result.fail("只能操作本院批次");
        }

        if (!ReimburseConstants.BATCH_STATUS_PENDING.equals(batch.getStatus())) {
            return Result.fail("批次状态不允许添加结算单");
        }

        Settle settle = settleService.getById(settleId);
        if (settle == null) {
            return Result.fail("结算单不存在");
        }
        if (!batch.getHospitalId().equals(settle.getHospitalId())) {
            return Result.fail("结算单不属于当前批次医院");
        }
        Integer settleStatus = settle.getStatus();
        if (ReimburseConstants.SETTLE_STATUS_UNDECLARED.equals(settleStatus)) {
            // 患者尚未支付自付金额，检查是否有自付金额
            if (settle.getSelfPay() != null && settle.getSelfPay().compareTo(BigDecimal.ZERO) > 0) {
                return Result.fail("患者尚未支付自付金额，无法加入批次");
            }
        } else if (!ReimburseConstants.SETTLE_STATUS_SELF_PAID.equals(settleStatus)) {
            return Result.fail("结算单状态不允许添加到批次");
        }

        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BatchItem::getSettleId, settleId);
        BatchItem existItem = batchItemService.getOne(itemWrapper);
        if (existItem != null) {
            return Result.fail("该结算单已添加到其他批次");
        }

        BatchItem batchItem = new BatchItem();
        batchItem.setBatchId(batchId);
        batchItem.setSettleId(settleId);
        batchItem.setAudit(ReimburseConstants.AUDIT_PASS);
        batchItem.setCreateTime(LocalDateTime.now());
        batchItemService.save(batchItem);

        int newSettleCnt = batch.getSettleCnt() + 1;
        BigDecimal newTotalAmt = batch.getTotalAmt().add(settle.getReimburse());

        batch.setSettleCnt(newSettleCnt);
        batch.setTotalAmt(newTotalAmt);
        this.updateById(batch);

        settle.setStatus(ReimburseConstants.SETTLE_STATUS_DECLARED);
        settleService.updateById(settle);

        // 注意：幂等标记已移到事务提交后（外层 addSettleToBatch 方法中）
        log.info("添加结算单到批次成功，batchId:{} settleId:{}", batchId, settleId);
        return Result.ok("添加成功");
    }

    // 查询批次详情及明细（含三级关联：结算单 -> 就诊 -> 患者），Redis 缓存加速组装
    @Override
    public Result getBatchDetail(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        // 权限校验：医保局/管理员可查看任意批次；医院仅本院
        String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
        boolean isAdmin = roleStr != null && String.valueOf(Role.ADMIN.getCode()).equals(roleStr);
        boolean isMedical = roleStr != null && String.valueOf(Role.MEDICAL.getCode()).equals(roleStr);
        Long currentHospitalId = UserHolder.getHospitalId();
        if (!isAdmin && !isMedical) {
            if (currentHospitalId == null) {
                return Result.fail("无权限查询该批次详情");
            }
            if (!currentHospitalId.equals(batch.getHospitalId())) {
                return Result.fail("只能查询本院批次");
            }
        }

        // 查询批次明细
        LambdaQueryWrapper<BatchItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchItem::getBatchId, batchId);
        List<BatchItem> items = batchItemService.list(wrapper);

        List<BatchItemVO> itemVOList = new ArrayList<>();

        if (!items.isEmpty()) {
            // 批量查询结算单 — Redis 缓存 + stream
            Set<Long> settleIds = items.stream().map(BatchItem::getSettleId).collect(Collectors.toSet());
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

                    // 通过就诊记录关联患者姓名
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

                itemVOList.add(vo);
            }
        }

        BatchVO vo = new BatchVO();
        BeanUtils.copyProperties(batch, vo);
        vo.setBatchItems(itemVOList);

        // 被拒付的批次 -> 查询拒付原因一并返回
        if (ReimburseConstants.BATCH_STATUS_PAY_REJECTED.equals(batch.getStatus())) {
            Pay pay = payService.getOne(new LambdaQueryWrapper<Pay>()
                    .eq(Pay::getBatchId, batchId)
                    .eq(Pay::getStatus, ReimburseConstants.PAY_STATUS_REJECTED));
            if (pay != null) {
                vo.setRejectReason(pay.getRejectReason());
            }
        }

        Hospital hospital = hospitalService.getById(batch.getHospitalId());
        if (hospital != null) {
            vo.setHospitalName(hospital.getName());
        }

        return Result.ok(vo);
    }

    // 医院端批次列表，分页 + 多条件过滤
    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO, Long batchId, LocalDateTime startTime, LocalDateTime endTime) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        LambdaQueryWrapper<Batch> wrapper = new LambdaQueryWrapper<Batch>()
                .eq(Batch::getHospitalId, hospitalId);
        if (batchId != null) {
            wrapper.eq(Batch::getId, batchId);
        }
        if (startTime != null) {
            wrapper.ge(Batch::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Batch::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Batch::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Batch> page =
            this.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageDTO.getPageNum(), pageDTO.getPageSize()), wrapper);

        List<BatchVO> batchVOList = enrichBatchVOList(page.getRecords());

        com.xxj.insurance.common.domain.PageDTO resultPageDTO = new com.xxj.insurance.common.domain.PageDTO(
            pageDTO.getPageNum(), pageDTO.getPageSize(), page.getTotal(), batchVOList
        );

        return Result.ok(resultPageDTO);
    }

    // 医保局端批次列表，分页 + 多条件过滤
    @Override
    public Result medicalList(PageDTO pageDTO, Long batchId, Long hospitalId, LocalDateTime startTime, LocalDateTime endTime) {
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        LambdaQueryWrapper<Batch> wrapper = new LambdaQueryWrapper<>();
        if (batchId != null) {
            wrapper.eq(Batch::getId, batchId);
        }
        if (hospitalId != null) {
            wrapper.eq(Batch::getHospitalId, hospitalId);
        }
        if (startTime != null) {
            wrapper.ge(Batch::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Batch::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Batch::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Batch> page =
            this.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageDTO.getPageNum(), pageDTO.getPageSize()), wrapper);

        List<BatchVO> batchVOList = enrichBatchVOList(page.getRecords());

        com.xxj.insurance.common.domain.PageDTO resultPageDTO = new com.xxj.insurance.common.domain.PageDTO(
            pageDTO.getPageNum(), pageDTO.getPageSize(), page.getTotal(), batchVOList
        );

        return Result.ok(resultPageDTO);
    }

    /**
     * 批量查询医院名称，组装 BatchVO
     */
    private List<BatchVO> enrichBatchVOList(List<Batch> batches) {
        if (batches == null || batches.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询医院名称 — Redis 缓存
        Set<Long> hospitalIds = batches.stream()
                .map(Batch::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Hospital> hospitalMap = new HashMap<>();
        for (Long id : hospitalIds) {
            String key = "cache:hospital:" + id;
            String json = redisTemplate.opsForValue().get(key);
            Hospital hospital = null;
            if (json != null) {
                try {
                    hospital = JSON.parseObject(json, Hospital.class);
                } catch (Exception e) {
                    log.warn("缓存解析失败 key:{}", key, e);
                }
            }
            if (hospital == null) {
                hospital = hospitalService.getById(id);
                if (hospital != null) {
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(hospital), 30, TimeUnit.MINUTES);
                }
            }
            if (hospital != null) {
                hospitalMap.put(id, hospital);
            }
        }

        return batches.stream().map(batch -> {
            BatchVO vo = new BatchVO();
            BeanUtils.copyProperties(batch, vo);
            Hospital hospital = hospitalMap.get(batch.getHospitalId());
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // 提交批次申报到医保局，锁在前 -> 事务在内 -> 幂等在后，三层安全防护
    @Override
    public Result declareBatch(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        String lockKey = "lock:batch:declare:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:batch:declare:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 第一层拦截：Redis 快速判断
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已申报，请勿重复提交");
            }

            // 抢分布式锁
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 锁内执行业务（带事务）
            Result result = transactionTemplate.execute(status -> executeDeclareBatchWithTransaction(batchId, idempotentBucket));

            // 事务提交成功后才写入幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("申报批次被中断，batchId:{}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：执行申报业务逻辑
     */
    private Result executeDeclareBatchWithTransaction(Long batchId, RBucket<String> idempotentBucket) {
        // 第二层拦截：锁内再次校验幂等
        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已申报，请勿重复提交");
        }

        // 第三层拦截：数据库校验
        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        if (!ReimburseConstants.BATCH_STATUS_PENDING.equals(batch.getStatus())) {
            return Result.fail("只有待申报状态的批次才能申报");
        }

        Result permissionError = verifyHospitalBatchOperatePermission(batch, "申报");
        if (permissionError != null) {
            return permissionError;
        }

        if (batch.getSettleCnt() == null || batch.getSettleCnt() == 0) {
            return Result.fail("批次中没有结算单，无法申报");
        }

        // 检查批次中所有结算单是否已被患者付款
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BatchItem::getBatchId, batchId);
        List<BatchItem> items = batchItemService.list(itemWrapper);
        List<Long> settleIds = new ArrayList<>();
        for (BatchItem item : items) {
            settleIds.add(item.getSettleId());
        }
        List<Settle> settles = settleService.listByIds(settleIds);
        for (Settle settle : settles) {
            if (settle.getSelfPay() != null && settle.getSelfPay().compareTo(BigDecimal.ZERO) > 0
                    && settle.getStatus() < ReimburseConstants.SETTLE_STATUS_SELF_PAID) {
                return Result.fail("批次中存在患者未付款的结算单，无法申报");
            }
        }

        batch.setStatus(ReimburseConstants.BATCH_STATUS_DECLARED);
        this.updateById(batch);

        log.info("批次申报成功，batchId:{}", batchId);
        return Result.ok("申报成功");
    }

    // 撤回已申报或被医保局拒绝的批次，锁 + 幂等 + 事务三保险
    @Override
    public Result withdrawBatch(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        String lockKey = "lock:batch:withdraw:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:batch:withdraw:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 第一层拦截：Redis 快速判断
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已撤回，请勿重复提交");
            }

            // 抢分布式锁
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 锁内执行业务（带事务）
            Result result = transactionTemplate.execute(status -> executeWithdrawBatchWithTransaction(batchId, idempotentBucket));

            // 事务提交成功后才写入幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("撤回申报被中断，batchId:{}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：执行撤回业务逻辑
     */
    private Result executeWithdrawBatchWithTransaction(Long batchId, RBucket<String> idempotentBucket) {
        // 第二层拦截：锁内再次校验幂等
        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已撤回，请勿重复提交");
        }

        // 第三层拦截：数据库校验
        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        if (!ReimburseConstants.BATCH_STATUS_DECLARED.equals(batch.getStatus())
            && !ReimburseConstants.BATCH_STATUS_PAY_REJECTED.equals(batch.getStatus())) {
            return Result.fail("只有已申报或被拒绝的批次才能撤回");
        }

        Result permissionError = verifyHospitalBatchOperatePermission(batch, "撤回");
        if (permissionError != null) {
            return permissionError;
        }

        Pay pay = payService.getOne(new LambdaQueryWrapper<Pay>().eq(Pay::getBatchId, batchId));
        if (pay != null && ReimburseConstants.PAY_STATUS_PAID.equals(pay.getStatus())) {
            return Result.fail("该批次已拨付，无法撤回");
        }

        batch.setStatus(ReimburseConstants.BATCH_STATUS_PENDING);
        this.updateById(batch);

        log.info("批次撤回申报成功，batchId:{}", batchId);
        return Result.ok("撤回成功");
    }

    /**
     * 校验医院/管理员对本院批次的操作权限
     */
    private Result verifyHospitalBatchOperatePermission(Batch batch, String actionName) {
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能" + actionName + "批次");
            }
        } else if (!currentHospitalId.equals(batch.getHospitalId())) {
            return Result.fail("只能" + actionName + "本院批次");
        }
        return null;
    }

    // 删除待申报批次（同步恢复结算单状态为未申报），锁 + 幂等 + 事务三保险
    @Override
    public Result deleteBatch(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        String lockKey = "lock:batch:delete:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:batch:delete:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 第一层拦截：Redis 快速判断
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已删除，请勿重复提交");
            }

            // 抢分布式锁
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 锁内执行业务（带事务）
            Result result = transactionTemplate.execute(status -> executeDeleteBatchWithTransaction(batchId, idempotentBucket));

            // 事务提交成功后才写入幂等标记
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            }

            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("删除批次被中断，batchId:{}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：执行删除批次业务逻辑
     */
    private Result executeDeleteBatchWithTransaction(Long batchId, RBucket<String> idempotentBucket) {
        // 第二层拦截：锁内再次校验幂等
        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已删除，请勿重复提交");
        }

        // 第三层拦截：数据库校验
        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        Result permissionError = verifyHospitalBatchOperatePermission(batch, "删除");
        if (permissionError != null) {
            return permissionError;
        }

        if (!ReimburseConstants.BATCH_STATUS_PENDING.equals(batch.getStatus())) {
            return Result.fail("只有待申报状态的批次才能删除");
        }

        // 查询关联的批次明细
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BatchItem::getBatchId, batchId);
        List<BatchItem> items = batchItemService.list(itemWrapper);

        // 恢复结算单状态为未申报
        if (!items.isEmpty()) {
            List<Long> settleIds = new ArrayList<>();
            for (BatchItem item : items) {
                settleIds.add(item.getSettleId());
            }
            List<Settle> settleList = settleService.listByIds(settleIds);
            List<Settle> toUpdate = new ArrayList<>();
            for (Settle settle : settleList) {
                // 只恢复"已申报"的结算单，"已自付"的保持不动
                if (ReimburseConstants.SETTLE_STATUS_DECLARED.equals(settle.getStatus())) {
                    settle.setStatus(ReimburseConstants.SETTLE_STATUS_UNDECLARED);
                    toUpdate.add(settle);
                }
            }
            if (!toUpdate.isEmpty()) {
                settleService.updateBatchById(toUpdate);
            }
            // 删除批次明细
            List<Long> itemIds = new ArrayList<>();
            for (BatchItem item : items) {
                itemIds.add(item.getId());
            }
            batchItemService.removeByIds(itemIds);
        }

        // 删除批次
        this.removeById(batchId);

        log.info("批次删除成功，batchId:{}", batchId);
        return Result.ok("删除成功");
    }

    // 查询待申报批次列表，供结算单选择加入
    @Override
    public Result pendingList(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        LambdaQueryWrapper<Batch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Batch::getHospitalId, hospitalId)
               .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_PENDING)
               .orderByDesc(Batch::getCreateTime);

        List<Batch> batches = this.list(wrapper);

        List<BatchVO> batchVOList = batches.stream().map(batch -> {
            BatchVO vo = new BatchVO();
            BeanUtils.copyProperties(batch, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(batchVOList);
    }
}
