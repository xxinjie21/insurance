package com.xxj.insurance.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.ReimburseRule;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.po.YearAccumulate;
import com.xxj.insurance.domain.vo.FeeDetailVO;
import com.xxj.insurance.domain.vo.SettleVO;
import com.xxj.insurance.mapper.SettleMapper;
import com.xxj.insurance.mapper.YearAccumulateMapper;
import com.xxj.insurance.service.IFeeService;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IReimburseRuleService;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IUserService;
import com.xxj.insurance.service.IVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 医保结算 Service 实现类
 * <p>
 * 核心功能：
 * 1. 医保结算计算：根据费用明细计算报销金额
 * 2. 幂等性控制：防止重复结算（Redis 预检查 + 分布式锁 + DB 校验）
 * 3. 事务控制：确保数据一致性
 * <p>
 * 修复记录：
 * - 事务回滚 NPE：transactionTemplate.execute() 返回 null 时兜底
 * - 幂等标识写入时机：移到事务提交成功后
 * - 缓存删除时机：移到事务提交成功后
 * - 分布式锁 leaseTime：-1 改为 30 秒，避免看门狗导致意外续期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleServiceImpl extends ServiceImpl<SettleMapper, Settle> implements ISettleService {

    private final IFeeService feeService;
    private final IVisitService visitService;
    private final IUserService userService;
    private final IHospitalService hospitalService;
    private final IReimburseRuleService reimburseRuleService;
    private final YearAccumulateMapper yearAccumulateMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final long LOCK_LEASE_TIME = 30; // 锁持有时间 30 秒

    // 医保结算：分布式锁 + 幂等防重 + 事务
    @Override
    public Result calculate(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        String lockKey = "lock:settle:" + visitId;
        RLock lock = redissonClient.getLock(lockKey);

        String idempotentKey = "idempotent:settle:visit:" + visitId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 第一层拦截：Redis 快速判断
            if (idempotentBucket.isExists()) {
                return Result.fail("该就诊已结算，请勿重复提交");
            }

            // 抢分布式锁，防止重复结算
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 锁内执行业务（带事务）
            Result result = transactionTemplate.execute(status -> executeCalculateWithTransaction(visitId, idempotentBucket));

            // 事务提交成功后才写入幂等标记和清理缓存
            if (result != null && result.getSuccess()) {
                idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
                // 异步绑定患者到医院（自动加入本院患者列表）
                Visit visit = getVisitWithCache(visitId);
                if (visit != null) {
                    userService.asyncBindHospital(visit.getUserId(), visit.getHospitalId());
                }
            }

            // 兜底：事务回滚时 transactionTemplate.execute() 可能返回 null
            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("结算操作被中断，就诊ID:{}", visitId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：执行结算业务逻辑
     * 注意：幂等标记和缓存删除已移到事务提交后（外层 calculate 方法中）
     */
    // 事务内执行结算（幂等校验 + 报销计算 + 保存）
    public Result executeCalculateWithTransaction(Long visitId, RBucket<String> idempotentBucket) {
        log.info("开始结算，就诊ID:{}", visitId);

        // 第二层拦截：锁内再次校验幂等
        if (idempotentBucket.isExists()) {
            return Result.fail("该就诊已结算，请勿重复提交");
        }

        // 第三层拦截：数据库校验
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.eq(Settle::getVisitId, visitId);
        Settle existSettle = this.getOne(settleWrapper);
        if (existSettle != null) {
            return Result.fail("该就诊已结算，请勿重复结算");
        }

        // 查就诊信息（走缓存）
        Visit visit = getVisitWithCache(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        // 权限校验：使用 Role 枚举替代魔术数字
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能进行结算");
            }
        } else if (!currentHospitalId.equals(visit.getHospitalId())) {
            return Result.fail("只能结算本院就诊记录");
        }

        // 查询费用明细
        LambdaQueryWrapper<Fee> feeWrapper = new LambdaQueryWrapper<>();
        feeWrapper.eq(Fee::getVisitId, visitId);
        List<Fee> feeList = feeService.list(feeWrapper);

        if (feeList == null || feeList.isEmpty()) {
            return Result.fail("该就诊暂无费用明细");
        }

        // 获取患者参保信息
        User patient = userService.getById(visit.getUserId());
        if (patient == null || patient.getInsuranceType() == null) {
            return Result.fail("患者参保信息不完整，无法结算");
        }

        // 获取医院等级
        Hospital hospital = hospitalService.getById(visit.getHospitalId());
        if (hospital == null || hospital.getLevel() == null) {
            return Result.fail("医院等级信息缺失，无法结算");
        }

        // 查询报销规则
        ReimburseRule rule = reimburseRuleService.findRule(
                patient.getInsuranceType(), hospital.getLevel(), visit.getType());
        if (rule == null) {
            return Result.fail("未找到匹配的报销规则");
        }

        // ---- 报销规则引擎计算 ----

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal catBSelfPayTotal = BigDecimal.ZERO; // 乙类先自付总额
        BigDecimal totalA = BigDecimal.ZERO;  // 甲类总额
        BigDecimal totalC = BigDecimal.ZERO;  // 自费类总额

        for (Fee fee : feeList) {
            if (fee.getTotal() == null) {
                continue;
            }
            totalAmount = totalAmount.add(fee.getTotal());

            // 乙类药先自付 category_b_self_ratio，剩余进入报销计算
            if (fee.getType() != null && fee.getType() == 2) {
                BigDecimal selfPart = fee.getTotal().multiply(rule.getCategoryBSelfRatio());
                catBSelfPayTotal = catBSelfPayTotal.add(selfPart);
            }
            if (fee.getType() != null && fee.getType() == 1) {
                totalA = totalA.add(fee.getTotal());
            }
            if (fee.getType() != null && fee.getType() == 3) {
                totalC = totalC.add(fee.getTotal());
            }
        }

        // 可报销基数 = 总费用 - 乙类先自付 - 自费类
        BigDecimal reimbursableBase = totalAmount.subtract(catBSelfPayTotal).subtract(totalC);

        // 起付线计算（年度累计维度）
        int currentYear = LocalDateTime.now().getYear();
        YearAccumulate accumulate = getOrCreateYearAccumulate(visit.getUserId(), currentYear);
        BigDecimal deductibleRemaining = rule.getDeductible().subtract(accumulate.getDeductibleUsed());
        if (deductibleRemaining.compareTo(BigDecimal.ZERO) < 0) {
            deductibleRemaining = BigDecimal.ZERO;
        }

        // 实际本次可报销金额 = max(0, 可报销基数 - 剩余起付线)
        BigDecimal actualReimbursable = reimbursableBase.subtract(deductibleRemaining);
        if (actualReimbursable.compareTo(BigDecimal.ZERO) < 0) {
            actualReimbursable = BigDecimal.ZERO;
        }

        // 统筹支付 = 实际可报销金额 × 报销比例
        BigDecimal poolingPay = actualReimbursable.multiply(rule.getReimburseRatio());

        // 封顶线校验
        BigDecimal capRemaining = rule.getAnnualCap().subtract(accumulate.getPoolingTotal());
        if (poolingPay.compareTo(capRemaining) > 0) {
            poolingPay = capRemaining;
        }
        if (poolingPay.compareTo(BigDecimal.ZERO) < 0) {
            poolingPay = BigDecimal.ZERO;
        }
        poolingPay = poolingPay.setScale(2, RoundingMode.HALF_UP);

        // 实际消耗的起付线 = min(剩余起付线, 可报销基数)
        BigDecimal deductibleConsumed = reimbursableBase.compareTo(deductibleRemaining) < 0
                ? reimbursableBase : deductibleRemaining;
        if (deductibleConsumed.compareTo(BigDecimal.ZERO) < 0) {
            deductibleConsumed = BigDecimal.ZERO;
        }
        deductibleConsumed = deductibleConsumed.setScale(2, RoundingMode.HALF_UP);

        // 个人账户支付：优先使用个人账户余额
        BigDecimal selfPayTotal = totalAmount.subtract(poolingPay).setScale(2, RoundingMode.HALF_UP);
        BigDecimal accountBalance = patient.getPersonalAccountBalance() != null
                ? patient.getPersonalAccountBalance() : BigDecimal.ZERO;
        BigDecimal accountPay = selfPayTotal.compareTo(accountBalance) <= 0
                ? selfPayTotal : accountBalance;
        accountPay = accountPay.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cashPay = selfPayTotal.subtract(accountPay).setScale(2, RoundingMode.HALF_UP);

        // 更新年度累计
        accumulate.setDeductibleUsed(accumulate.getDeductibleUsed().add(deductibleConsumed));
        accumulate.setPoolingTotal(accumulate.getPoolingTotal().add(poolingPay));
        yearAccumulateMapper.updateById(accumulate);

        // 保存结算记录
        Settle settle = new Settle();
        settle.setVisitId(visitId);
        settle.setHospitalId(visit.getHospitalId());
        settle.setTotal(totalAmount);
        settle.setReimburse(poolingPay);  // reimburse 保持为统筹支付金额
        settle.setSelfPay(selfPayTotal);
        settle.setPoolingPay(poolingPay);
        settle.setAccountPay(accountPay);
        settle.setCashPay(cashPay);
        settle.setStatus(ReimburseConstants.SETTLE_STATUS_UNDECLARED);
        settle.setCreateTime(LocalDateTime.now());
        this.save(settle);

        // 就诊状态不变，等患者付款结算后再更新为"已结算"

        // 注意：幂等标记和缓存删除已移到事务提交后
        log.info("结算成功，就诊ID:{}, 结算ID:{}, 统筹支付:{}, 个账支付:{}, 现金支付:{}",
                visitId, settle.getId(), poolingPay, accountPay, cashPay);
        return Result.ok(settle);
    }

    // 查询结算详情（含就诊、患者、医院信息 + 费用明细逐项拆分）
    @Override
    public Result getSettleDetail(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        Visit visit = getVisitWithCache(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId != null && !currentHospitalId.equals(visit.getHospitalId())) {
            return Result.fail("只能查询本院结算详情");
        }

        LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Settle::getVisitId, visitId);
        Settle settle = this.getOne(wrapper);

        if (settle == null) {
            return Result.fail("该就诊尚未结算");
        }

        List<SettleVO> voList = enrichSettleVOList(Collections.singletonList(settle));
        SettleVO vo = voList.isEmpty() ? null : voList.get(0);
        if (vo != null) {
            vo.setFeeDetails(buildFeeDetails(visitId));
        }
        return Result.ok(vo);
    }

    // 构建费用明细拆分：每项费用按比例分配统筹/自付
    private List<FeeDetailVO> buildFeeDetails(Long visitId) {
        LambdaQueryWrapper<Fee> feeWrapper = new LambdaQueryWrapper<>();
        feeWrapper.eq(Fee::getVisitId, visitId);
        List<Fee> feeList = feeService.list(feeWrapper);

        // 计算参与统筹的总金额(甲+乙)
        BigDecimal totalAB = BigDecimal.ZERO;
        for (Fee fee : feeList) {
            if (fee.getTotal() != null && fee.getType() != null && fee.getType() != 3) {
                totalAB = totalAB.add(fee.getTotal());
            }
        }

        // 从 settle 获取统筹支付总额
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.eq(Settle::getVisitId, visitId);
        Settle settle = this.getOne(settleWrapper);
        BigDecimal poolingTotal = settle != null && settle.getPoolingPay() != null
                ? settle.getPoolingPay() : BigDecimal.ZERO;

        // 逐项分配
        List<FeeDetailVO> details = new ArrayList<>();
        for (Fee fee : feeList) {
            FeeDetailVO detail = new FeeDetailVO();
            detail.setId(fee.getId());
            detail.setName(fee.getName());
            detail.setInsuranceCode(fee.getInsuranceCode());
            detail.setSpecification(fee.getSpecification());
            detail.setNum(fee.getNum());
            detail.setPrice(fee.getPrice());
            detail.setTotal(fee.getTotal());
            detail.setType(fee.getType());

            if (fee.getType() != null && fee.getType() != 3 && fee.getTotal() != null
                    && totalAB.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = fee.getTotal().divide(totalAB, 6, RoundingMode.HALF_UP);
                BigDecimal itemReimburse = poolingTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                detail.setReimburse(itemReimburse);
                detail.setSelfPay(fee.getTotal().subtract(itemReimburse).setScale(2, RoundingMode.HALF_UP));
            } else {
                detail.setReimburse(BigDecimal.ZERO);
                detail.setSelfPay(fee.getTotal() != null ? fee.getTotal() : BigDecimal.ZERO);
            }
            details.add(detail);
        }
        return details;
    }

    // 查询或创建年度累计记录
    private YearAccumulate getOrCreateYearAccumulate(Long userId, int year) {
        LambdaQueryWrapper<YearAccumulate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(YearAccumulate::getUserId, userId)
               .eq(YearAccumulate::getYear, year);
        YearAccumulate accumulate = yearAccumulateMapper.selectOne(wrapper);
        if (accumulate == null) {
            accumulate = new YearAccumulate();
            accumulate.setUserId(userId);
            accumulate.setYear(year);
            accumulate.setDeductibleUsed(BigDecimal.ZERO);
            accumulate.setPoolingTotal(BigDecimal.ZERO);
            accumulate.setCreateTime(LocalDateTime.now());
            accumulate.setUpdateTime(LocalDateTime.now());
            yearAccumulateMapper.insert(accumulate);
        }
        return accumulate;
    }

    // 查就诊（Redis 缓存，防缓存穿透）
    private Visit getVisitWithCache(Long visitId) {
        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visitId;

        String cacheJson = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheJson)) {
            try {
                log.info("从缓存获取就诊信息，visitId:{}", visitId);
                return JSON.parseObject(cacheJson, Visit.class);
            } catch (Exception e) {
                log.warn("缓存解析失败 key:{}", cacheKey, e);
            }
        }

        // 缓存不存在，查数据库
        Visit visit = visitService.getById(visitId);

        if (visit != null) {
            // 查到了，写正常缓存
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(visit),
                    RedisConstants.CACHE_VISIT_TTL, TimeUnit.MINUTES);
        } else {
            // 查不到也写缓存（空值），防穿透
            redisTemplate.opsForValue().set(cacheKey, "", 1, TimeUnit.MINUTES);
            log.info("防穿透缓存空值, visitId:{}", visitId);
        }

        return visit;
    }

    /**
     * 患者端：查询本人的结算记录列表
     * 流程：先根据用户ID查到关联的就诊ID -> 再根据就诊ID查询结算记录 -> 分页返回
     */
    @Override
    public Result myList(Long userId, PageDTO pageDTO, Long hospitalId, LocalDateTime startTime, LocalDateTime endTime) {
        // 校验用户ID不能为空
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        // 分页参数为空时，给默认值：第1页，每页10条
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        // 1. 构建查询条件：只查当前用户的就诊记录
        LambdaQueryWrapper<Visit> visitWrapper = new LambdaQueryWrapper<Visit>()
                .eq(Visit::getUserId, userId);
        // 传入医院ID则追加条件
        if (hospitalId != null) {
            visitWrapper.eq(Visit::getHospitalId, hospitalId);
        }

        // 查询当前用户所有符合条件的就诊ID
        List<Long> visitIds = visitService.list(visitWrapper)
                .stream()
                .map(Visit::getId)
                .collect(Collectors.toList());

        // 无就诊记录直接返回空列表
        if (visitIds.isEmpty()) {
            return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
        }

        // 2. 根据就诊ID查询结算记录
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.in(Settle::getVisitId, visitIds);
        // 时间范围筛选
        if (startTime != null) {
            settleWrapper.ge(Settle::getCreateTime, startTime);
        }
        if (endTime != null) {
            settleWrapper.le(Settle::getCreateTime, endTime);
        }
        // 按结算时间倒序排列
        settleWrapper.orderByDesc(Settle::getCreateTime);

        // 执行分页查询
        Page<Settle> page = page(new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize()), settleWrapper);

        // 封装VO并补充关联信息（如就诊、患者、医院等）
        List<SettleVO> voList = enrichSettleVOList(page.getRecords());

        // 返回统一分页格式
        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), page.getTotal(), voList));
    }

    /**
     * 医院端查询结算列表
     * 支持：患者ID、患者姓名、时间范围、医院筛选
     */
    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO, String patientName, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 校验医院ID
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        // 分页参数默认值
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        // 基础条件：当前医院
        LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<Settle>()
                .eq(Settle::getHospitalId, hospitalId);

        // 按患者ID筛选
        if (userId != null) {
            List<Long> visitIds = visitService.lambdaQuery()
                    .eq(Visit::getUserId, userId)
                    .list()
                    .stream()
                    .map(Visit::getId)
                    .collect(Collectors.toList());

            if (visitIds.isEmpty()) {
                return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
            }

            wrapper.in(Settle::getVisitId, visitIds);
        }

        // 按患者姓名筛选
        else if (StrUtil.isNotBlank(patientName)) {
            // 先查用户ID
            List<Long> userIds = userService.lambdaQuery()
                    .like(User::getName, patientName)
                    .list()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (userIds.isEmpty()) {
                return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
            }

            // 再查就诊ID
            List<Long> visitIds = visitService.lambdaQuery()
                    .in(Visit::getUserId, userIds)
                    .list()
                    .stream()
                    .map(Visit::getId)
                    .collect(Collectors.toList());

            if (visitIds.isEmpty()) {
                return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
            }

            wrapper.in(Settle::getVisitId, visitIds);
        }

        // 时间范围
        if (startTime != null) {
            wrapper.ge(Settle::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Settle::getCreateTime, endTime);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Settle::getCreateTime);

        // 分页查询
        Page<Settle> page = page(new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize()), wrapper);

        List<Settle> records = page.getRecords();
        if (records.isEmpty()) {
            return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), page.getTotal(), Collections.emptyList()));
        }

        // 组装VO信息
        List<SettleVO> voList = enrichSettleVOList(records);

        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), page.getTotal(), voList));
    }

    // 批量组装结算单 VO：ID 聚合 → 批量加载 → Map 映射，SQL O(N)→O(1)
    private List<SettleVO> enrichSettleVOList(List<Settle> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. ID 聚合：收集所有 visitId
        Set<Long> visitIds = records.stream()
                .map(Settle::getVisitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 批量加载就诊：缓存命中直接入 Map，未命中收集 → listByIds 批量 IN 查询 → 写回缓存
        Map<Long, Visit> visitMap = new HashMap<>();
        List<Long> missedVisitIds = new ArrayList<>();
        for (Long id : visitIds) {
            String key = "cache:visit:" + id;
            String json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                try {
                    visitMap.put(id, JSON.parseObject(json, Visit.class));
                } catch (Exception e) {
                    log.warn("缓存解析失败 key:{}", key, e);
                    missedVisitIds.add(id);
                }
            } else {
                missedVisitIds.add(id);
            }
        }
        if (!missedVisitIds.isEmpty()) {
            List<Visit> visitList = visitService.listByIds(missedVisitIds);
            for (Visit visit : visitList) {
                visitMap.put(visit.getId(), visit);
                redisTemplate.opsForValue().set("cache:visit:" + visit.getId(),
                        JSON.toJSONString(visit), RedisConstants.CACHE_VISIT_TTL, TimeUnit.MINUTES);
            }
            // 缓存空值防穿透
            for (Long id : missedVisitIds) {
                if (!visitMap.containsKey(id)) {
                    redisTemplate.opsForValue().set("cache:visit:" + id, "", 1, TimeUnit.MINUTES);
                }
            }
        }

        // 3. ID 聚合：从 visitMap 收集所有 userId
        Set<Long> userIds = visitMap.values().stream()
                .map(Visit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. 批量加载患者：缓存命中直接入 Map，未命中收集 → listByIds 批量 IN 查询 → 写回缓存
        Map<Long, User> userMap = new HashMap<>();
        List<Long> missedUserIds = new ArrayList<>();
        for (Long id : userIds) {
            String key = "cache:user:" + id;
            String json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                try {
                    userMap.put(id, JSON.parseObject(json, User.class));
                } catch (Exception e) {
                    log.warn("缓存解析失败 key:{}", key, e);
                    missedUserIds.add(id);
                }
            } else {
                missedUserIds.add(id);
            }
        }
        if (!missedUserIds.isEmpty()) {
            List<User> userList = userService.listByIds(missedUserIds);
            for (User user : userList) {
                userMap.put(user.getId(), user);
                redisTemplate.opsForValue().set("cache:user:" + user.getId(),
                        JSON.toJSONString(user), 30, TimeUnit.MINUTES);
            }
            for (Long id : missedUserIds) {
                if (!userMap.containsKey(id)) {
                    redisTemplate.opsForValue().set("cache:user:" + id, "", 1, TimeUnit.MINUTES);
                }
            }
        }

        // 5. ID 聚合：从 records 收集所有 hospitalId
        Set<Long> hospitalIds = records.stream()
                .map(Settle::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 6. 批量加载医院：缓存命中直接入 Map，未命中收集 → listByIds 批量 IN 查询 → 写回缓存
        Map<Long, Hospital> hospitalMap = new HashMap<>();
        List<Long> missedHospitalIds = new ArrayList<>();
        for (Long id : hospitalIds) {
            String key = "cache:hospital:" + id;
            String json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                try {
                    hospitalMap.put(id, JSON.parseObject(json, Hospital.class));
                } catch (Exception e) {
                    log.warn("缓存解析失败 key:{}", key, e);
                    missedHospitalIds.add(id);
                }
            } else {
                missedHospitalIds.add(id);
            }
        }
        if (!missedHospitalIds.isEmpty()) {
            List<Hospital> hospitalList = hospitalService.listByIds(missedHospitalIds);
            for (Hospital hospital : hospitalList) {
                hospitalMap.put(hospital.getId(), hospital);
                redisTemplate.opsForValue().set("cache:hospital:" + hospital.getId(),
                        JSON.toJSONString(hospital), 30, TimeUnit.MINUTES);
            }
            for (Long id : missedHospitalIds) {
                if (!hospitalMap.containsKey(id)) {
                    redisTemplate.opsForValue().set("cache:hospital:" + id, "", 1, TimeUnit.MINUTES);
                }
            }
        }

        // 7. Map 映射：组装 VO
        List<SettleVO> voList = new ArrayList<>();
        for (Settle settle : records) {
            SettleVO vo = new SettleVO();
            BeanUtils.copyProperties(settle, vo);

            Visit visit = visitMap.get(settle.getVisitId());
            if (visit != null) {
                vo.setVisitType(visit.getType());
                vo.setDiagnosis(visit.getDiagnosis());
                User user = userMap.get(visit.getUserId());
                fillPatientIdCard(vo, user);
            }

            if (settle.getHospitalId() != null) {
                Hospital hospital = hospitalMap.get(settle.getHospitalId());
                if (hospital != null) {
                    vo.setHospitalName(hospital.getName());
                }
            }

            voList.add(vo);
        }
        return voList;
    }

    // 填充患者姓名和身份证号
    private void fillPatientIdCard(SettleVO vo, User user) {
        if (vo == null || user == null) {
            return;
        }
        vo.setPatientName(user.getName());
        String idCard = user.getIdCard();
        vo.setPatientIdCard(idCard);
        vo.setIdCard(idCard);
    }

    // 查询可加入批次的结算单（待申报 + 已自付），复用 enrichSettleVOList 批量加载
    @Override
    public Result availableForBatch(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        // 查询本院可加入批次的结算单（待申报 + 已自付）
        LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Settle::getHospitalId, hospitalId)
                .in(Settle::getStatus, ReimburseConstants.SETTLE_STATUS_UNDECLARED, ReimburseConstants.SETTLE_STATUS_SELF_PAID)
                .orderByDesc(Settle::getCreateTime);
        List<Settle> settles = this.list(wrapper);

        if (settles.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // ID 聚合 → 批量加载 → Map 映射（复用批量查询方法）
        List<SettleVO> voList = enrichSettleVOList(settles);
        return Result.ok(voList);
    }
}
