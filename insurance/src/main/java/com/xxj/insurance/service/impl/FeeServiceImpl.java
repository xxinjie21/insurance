package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.domain.po.ConsumableCatalog;
import com.xxj.insurance.domain.po.DrugCatalog;
import com.xxj.insurance.domain.po.TreatmentCatalog;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.FeeVO;
import com.xxj.insurance.mapper.FeeMapper;
import com.xxj.insurance.service.ICatalogService;
import com.xxj.insurance.service.IFeeService;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 费用模块业务实现类
 * 功能：费用新增、批量新增、查询、删除、权限控制、分页展示
 * 特点：分布式锁防并发、事务保证一致性、严格权限校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeServiceImpl extends ServiceImpl<FeeMapper, Fee> implements IFeeService {

    // 分布式锁自动释放时间（30秒），防止服务宕机导致死锁
    private static final long LOCK_LEASE_TIME = 30;

    // 分布式锁客户端
    private final RedissonClient redissonClient;
    // 就诊服务
    private final IVisitService visitService;
    // 医院服务
    private final IHospitalService hospitalService;
    // 目录服务
    private final ICatalogService catalogService;
    // 编程式事务
    private final TransactionTemplate transactionTemplate;
    // Redis（读取角色）
    private final StringRedisTemplate redisTemplate;

    /**
     * 批量新增费用明细
     * 带分布式锁 + 事务，防止同一就诊并发添加导致数据异常
     */
    @Override
    public Result batchAdd(List<FeeAddDTO> dtoList) {
        // 非空校验
        if (dtoList == null || dtoList.isEmpty()) {
            return Result.fail("费用明细不能为空");
        }

        // 从第一条数据获取就诊ID（同一批必须属于同一个就诊）
        Long visitId = dtoList.get(0).getVisitId();
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        // 分布式锁key：按就诊ID加锁，避免同一就诊并发操作
        String lockKey = "lock:fee:" + visitId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试抢锁：最多等10秒，持有30秒自动释放
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("正在处理费用，请稍后");
            }

            // 执行事务方法
            Result result = transactionTemplate.execute(status -> executeBatchAddWithTransaction(dtoList, visitId));
            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            // 线程中断处理
            Thread.currentThread().interrupt();
            return Result.fail("操作中断");
        } finally {
            // 确保只释放自己加的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内批量新增费用
     * 包含：权限校验、数据校验、金额计算、批量保存
     */
    public Result executeBatchAddWithTransaction(List<FeeAddDTO> dtoList, Long visitId) {
        // 校验就诊记录是否存在
        Visit visit = visitService.getById(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        // 权限校验：只有本院职工 / 管理员可添加
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            // 无医院ID，说明可能是管理员
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能添加费用");
            }
        } else if (!currentHospitalId.equals(visit.getHospitalId())) {
            // 有医院ID，但不是就诊所属医院
            return Result.fail("只能为本院就诊记录添加费用");
        }

        // 只有未结算的就诊才能添加费用
        if (!ReimburseConstants.VISIT_STATUS_PENDING.equals(visit.getStatus())) {
            return Result.fail("已结算就诊不能继续添加费用");
        }

        // 逐条校验费用明细参数
        for (FeeAddDTO dto : dtoList) {
            if (dto == null) {
                return Result.fail("费用明细不能为空");
            }
            // 必须属于同一个就诊
            if (!visitId.equals(dto.getVisitId())) {
                return Result.fail("同一批费用必须属于同一个就诊记录");
            }
            // 费用名称非空
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                return Result.fail("费用项目名称不能为空");
            }
            // 单价 > 0
            if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.fail("单价必须大于 0");
            }
            // 数量 > 0
            if (dto.getNum() == null || dto.getNum() <= 0) {
                return Result.fail("数量必须大于 0");
            }
            // 费用类型 1~3 有效
            if (dto.getType() == null || dto.getType() < 1 || dto.getType() > 3) {
                return Result.fail("费用类型无效");
            }
        }

        // 选目录时回填名称/类型/编码/规格
        for (FeeAddDTO dto : dtoList) {
            if (dto.getCatalogId() != null && dto.getCatalogType() != null) {
                fillFromCatalog(dto);
            }
        }

        // DTO 转 PO，计算总价：单价 × 数量
        List<Fee> feeList = dtoList.stream().map(dto -> {
            BigDecimal total = dto.getPrice().multiply(BigDecimal.valueOf(dto.getNum()));
            Fee fee = new Fee();
            BeanUtils.copyProperties(dto, fee);
            fee.setTotal(total);
            fee.setCreateTime(LocalDateTime.now());
            return fee;
        }).collect(Collectors.toList());

        // 批量保存费用
        saveBatch(feeList);

        // 查询医院名称，用于前端展示
        String hospitalName = null;
        if (visit.getHospitalId() != null) {
            Hospital hospital = hospitalService.getById(visit.getHospitalId());
            if (hospital != null) {
                hospitalName = hospital.getName();
            }
        }

        // 组装 VO 返回
        final String finalHospitalName = hospitalName;
        List<FeeVO> voList = feeList.stream().map(fee -> {
            FeeVO vo = new FeeVO();
            BeanUtils.copyProperties(fee, vo);
            vo.setHospitalName(finalHospitalName);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(voList);
    }

    /**
     * 根据就诊ID查询费用明细
     * 权限控制：只能看自己/本院的费用
     */
    @Override
    public Result listByVisitId(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊ID不能为空");
        }

        // 查询就诊记录
        Visit visit = visitService.getById(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        // 权限校验
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId != null) {
            // 医院人员：只能看本院
            if (!currentHospitalId.equals(visit.getHospitalId())) {
                return Result.fail("只能查看本院就诊的费用");
            }
        } else {
            // 患者：只能看自己的
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr != null && String.valueOf(Role.PATIENT.getCode()).equals(roleStr)) {
                if (!UserHolder.getUserId().equals(visit.getUserId())) {
                    return Result.fail("只能查看自己的费用");
                }
            }
        }

        // 查询费用列表
        List<Fee> feeList = lambdaQuery()
                .eq(Fee::getVisitId, visitId)
                .list();

        // 填充医院名称
        String hospitalName = null;
        if (visit.getHospitalId() != null) {
            Hospital hospital = hospitalService.getById(visit.getHospitalId());
            if (hospital != null) {
                hospitalName = hospital.getName();
            }
        }

        // 转VO
        final String finalHospitalName = hospitalName;
        List<FeeVO> voList = feeList.stream().map(fee -> {
            FeeVO vo = new FeeVO();
            BeanUtils.copyProperties(fee, vo);
            vo.setHospitalName(finalHospitalName);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(voList);
    }

    /**
     * 患者端：查询本人所有费用（分页）
     * 先查自己的就诊 → 再查费用 → 批量查医院名称展示
     */
    @Override
    public Result myList(Long userId, PageDTO pageDTO, Long visitId, LocalDateTime startTime, LocalDateTime endTime) {
        // 用户ID不能为空
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        // 分页参数默认值
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        // 条件：只查当前用户的就诊
        LambdaQueryWrapper<Visit> visitWrapper = new LambdaQueryWrapper<>();
        visitWrapper.eq(Visit::getUserId, userId);
        // 可按就诊ID精确筛选
        if (visitId != null) {
            visitWrapper.eq(Visit::getId, visitId);
        }
        List<Visit> visits = visitService.list(visitWrapper);

        // 无就诊直接返回空
        if (visits.isEmpty()) {
            return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), 0L, Collections.emptyList()));
        }

        // 提取所有就诊ID
        List<Long> visitIds = visits.stream().map(Visit::getId).collect(Collectors.toList());
        // 转Map，方便根据ID快速取就诊
        Map<Long, Visit> visitMap = visits.stream()
                .collect(Collectors.toMap(Visit::getId, Function.identity(), (a, b) -> a));

        // 批量查询医院，避免N+1
        Set<Long> hospitalIds = visits.stream()
                .map(Visit::getHospitalId)
                .collect(Collectors.toSet());
        Map<Long, Hospital> hospitalMap = hospitalService.listByIds(hospitalIds).stream()
                .collect(Collectors.toMap(Hospital::getId, Function.identity(), (a, b) -> a));

        // 费用查询条件
        LambdaQueryWrapper<Fee> feeWrapper = new LambdaQueryWrapper<>();
        feeWrapper.in(Fee::getVisitId, visitIds);
        // 时间范围
        if (startTime != null) {
            feeWrapper.ge(Fee::getCreateTime, startTime);
        }
        if (endTime != null) {
            feeWrapper.le(Fee::getCreateTime, endTime);
        }
        // 按时间倒序，最新在前
        feeWrapper.orderByDesc(Fee::getCreateTime);

        // 分页查询费用
        Page<Fee> feePage = page(new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize()), feeWrapper);

        // 组装VO，填充医院名称
        List<FeeVO> voList = feePage.getRecords().stream().map(fee -> {
            FeeVO vo = new FeeVO();
            BeanUtils.copyProperties(fee, vo);

            Visit visit = visitMap.get(fee.getVisitId());
            if (visit != null && visit.getHospitalId() != null) {
                Hospital hospital = hospitalMap.get(visit.getHospitalId());
                if (hospital != null) {
                    vo.setHospitalName(hospital.getName());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        // 返回分页数据
        return Result.ok(new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), feePage.getTotal(), voList));
    }

    /**
     * 删除费用
     * 权限：本院/管理员
     * 限制：只能删除未结算的费用
     */
    @Override
    public Result deleteFee(Long feeId) {
        if (feeId == null) {
            return Result.fail("费用 ID 不能为空");
        }

        // 查询费用是否存在
        Fee fee = getById(feeId);
        if (fee == null) {
            return Result.fail("费用记录不存在");
        }

        // 查询关联就诊
        Visit visit = visitService.getById(fee.getVisitId());
        if (visit == null) {
            return Result.fail("关联就诊记录不存在");
        }

        // 已结算不能删
        if (!ReimburseConstants.VISIT_STATUS_PENDING.equals(visit.getStatus())) {
            return Result.fail("已结算就诊的费用不能删除");
        }

        // 权限校验
        Long currentHospitalId = UserHolder.getHospitalId();
        if (currentHospitalId == null) {
            // 管理员可删
            String roleStr = redisTemplate.opsForValue().get("login:role:" + UserHolder.getUserId());
            if (roleStr == null || !String.valueOf(Role.ADMIN.getCode()).equals(roleStr)) {
                return Result.fail("只有医院角色或管理员才能删除费用");
            }
        } else if (!currentHospitalId.equals(visit.getHospitalId())) {
            // 非本院不能删
            return Result.fail("只能删除本院就诊的费用");
        }

        // 执行删除
        removeById(feeId);
        return Result.ok("删除成功");
    }

    /**
     * 从目录回填费用信息：名称、类别、医保编码、规格
     */
    private void fillFromCatalog(FeeAddDTO dto) {
        try {
            Long catalogId = dto.getCatalogId();
            String catalogType = dto.getCatalogType();
            if ("drug".equals(catalogType)) {
                DrugCatalog drug = catalogService.getDrugById(catalogId);
                if (drug != null) {
                    dto.setName(drug.getName());
                    dto.setType(drug.getCategory());
                    dto.setInsuranceCode(drug.getCode());
                    dto.setSpecification(drug.getSpecification());
                }
            } else if ("treatment".equals(catalogType)) {
                TreatmentCatalog treatment = catalogService.getTreatmentById(catalogId);
                if (treatment != null) {
                    dto.setName(treatment.getName());
                    dto.setType(treatment.getCategory());
                    dto.setInsuranceCode(treatment.getCode());
                }
            } else if ("consumable".equals(catalogType)) {
                ConsumableCatalog consumable = catalogService.getConsumableById(catalogId);
                if (consumable != null) {
                    dto.setName(consumable.getName());
                    dto.setType(consumable.getCategory());
                    dto.setInsuranceCode(consumable.getCode());
                    dto.setSpecification(consumable.getSpecification());
                }
            }
        } catch (Exception e) {
            log.warn("目录回填失败，使用手动录入值：catalogType={} catalogId={}", dto.getCatalogType(), dto.getCatalogId(), e);
        }
    }
}