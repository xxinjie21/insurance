package com.xxj.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.AccountConstants;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.RechargeDTO;
import com.xxj.insurance.domain.po.ConsumptionRecord;
import com.xxj.insurance.domain.po.RechargeRecord;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.UserAccount;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.ConsumptionRecordVO;
import com.xxj.insurance.domain.vo.RechargeRecordVO;
import com.xxj.insurance.domain.vo.UserAccountVO;
import com.xxj.insurance.mapper.ConsumptionRecordMapper;
import com.xxj.insurance.mapper.RechargeRecordMapper;
import com.xxj.insurance.mapper.UserAccountMapper;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IUserAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 患者账户 Service 实现类
 * 修复记录：
 * - 分布式锁 leaseTime: -1 改为 30 秒
 * - SecureRandom 改为类级别 static final
 * - 事务回滚 NPE 兜底
 * - 冗余查询：pay 方法中 settle 只查一次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements IUserAccountService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // 复用
    private static final long LOCK_LEASE_TIME = 30;

    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final ISettleService settleService;
    private final IVisitService visitService;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    // 查询用户账户，不存在则自动创建
    @Override
    public Result getAccount(Long userId) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);

        if (account == null) {
            account = createAccount(userId);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        UserAccountVO vo = new UserAccountVO();
        BeanUtils.copyProperties(account, vo);
        vo.setUserName(user.getName());

        return Result.ok(vo);
    }

    // 用户充值：参数校验 → 分布式锁 → 事务内插入充值记录 + 更新余额
    @Override
    public Result recharge(Long userId, RechargeDTO dto) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (dto == null || dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于 0");
        }

        if (dto.getType() == null || dto.getType() < 1 || dto.getType() > 4) {
            return Result.fail("充值类型无效");
        }

        // 分布式锁：防止并发操作同一账户余额
        String lockKey = "lock:account:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 等10秒拿不到锁则放弃，避免长时间阻塞
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 事务保证：充值记录和余额更新同进同退
            Result result = transactionTemplate.execute(status -> executeRechargeWithTransaction(userId, dto));
            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("充值操作被中断，userId: {}", userId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 充值事务体：插入充值记录 → 更新账户余额
    public Result executeRechargeWithTransaction(Long userId, RechargeDTO dto) {
        UserAccount account = getOrCreateAccount(userId);

        if (account.getStatus().equals(AccountConstants.ACCOUNT_STATUS_FROZEN)) {
            return Result.fail("账户已被冻结，无法充值");
        }

        String orderNo = generateRechargeOrderNo(userId);

        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setOrderNo(orderNo);
        record.setAmount(dto.getAmount());
        record.setType(dto.getType());
        record.setStatus(AccountConstants.RECHARGE_STATUS_SUCCESS);
        record.setPayTime(LocalDateTime.now());
        record.setRemark(dto.getRemark());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        rechargeRecordMapper.insert(record);

        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        BigDecimal newTotalRecharge = account.getTotalRecharge().add(dto.getAmount());

        account.setBalance(newBalance);
        account.setTotalRecharge(newTotalRecharge);
        account.setUpdateTime(LocalDateTime.now());

        this.updateById(account);

        log.info("充值成功，userId: {}, orderNo: {}, amount: {}", userId, orderNo, dto.getAmount());

        return Result.ok("充值成功");
    }

    // 支付就诊自付费用：校验 → 查结算单 → 分布式锁 → 事务扣款
    @Override
    public Result pay(Long userId, Long visitId, String remark) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        Visit visit = visitService.getById(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }
        if (!userId.equals(visit.getUserId())) {
            return Result.fail("只能支付自己的就诊费用");
        }

        // 查询结算单，获取自付金额
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.eq(Settle::getVisitId, visitId);
        Settle settle = settleService.getOne(settleWrapper);

        if (settle == null) {
            return Result.fail("该就诊尚未结算，无法支付");
        }

        BigDecimal payAmount = settle.getSelfPay();

        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("无需支付金额");
        }

        String lockKey = "lock:account:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 事务内执行扣款，settle 从外部传入避免重复查询
            Result result = transactionTemplate.execute(status -> executePayWithTransaction(userId, visitId, remark, payAmount, settle));
            return result != null ? result : Result.fail("操作失败，请重试");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("支付操作被中断，userId: {}", userId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行支付事务
     * 修复：settle 从外部传入，避免重复查询
     */
    // 支付事务体：扣个人账户余额 → 插消费记录 → 更新结算单/就诊状态
    public Result executePayWithTransaction(Long userId, Long visitId, String remark, BigDecimal payAmount, Settle settle) {
        // 结算单已标记自付完成则拒绝，防止重复扣款
        if (settle != null && settle.getStatus() >= ReimburseConstants.SETTLE_STATUS_SELF_PAID) {
            return Result.fail("该就诊已完成支付，请勿重复支付");
        }

        LambdaQueryWrapper<ConsumptionRecord> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(ConsumptionRecord::getUserId, userId)
                .eq(ConsumptionRecord::getVisitId, visitId)
                .eq(ConsumptionRecord::getType, AccountConstants.CONSUMPTION_TYPE_VISIT_PAY)
                .eq(ConsumptionRecord::getStatus, AccountConstants.CONSUMPTION_STATUS_SUCCESS);
        if (consumptionRecordMapper.selectCount(paidWrapper) > 0) {
            return Result.fail("该就诊已支付，请勿重复支付");
        }

        // 查询用户医保个人账户余额
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        BigDecimal personalBalance = user.getPersonalAccountBalance() != null
                ? user.getPersonalAccountBalance() : BigDecimal.ZERO;

        // 实际扣款金额：min(支付金额, 个人账户余额)
        BigDecimal actualDeduct = payAmount.compareTo(personalBalance) <= 0
                ? payAmount : personalBalance;

        String orderNo = generateConsumptionOrderNo(userId);

        BigDecimal balanceBefore = personalBalance;
        BigDecimal balanceAfter = personalBalance.subtract(actualDeduct);

        // 更新用户医保个人账户余额
        user.setPersonalAccountBalance(balanceAfter);
        userMapper.updateById(user);

        // 同步更新 user_account 表余额（兼容，模块五会全面改造）
        UserAccount account = getOrCreateAccount(userId);
        BigDecimal accountBalanceBefore = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal accountBalanceAfter = accountBalanceBefore.subtract(actualDeduct);
        if (accountBalanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            accountBalanceAfter = BigDecimal.ZERO;
        }
        account.setBalance(accountBalanceAfter);
        account.setTotalConsumption(account.getTotalConsumption().add(actualDeduct));
        account.setUpdateTime(LocalDateTime.now());
        this.updateById(account);

        // 消费记录（记录实际扣款来源：个人账户）
        ConsumptionRecord record = new ConsumptionRecord();
        record.setUserId(userId);
        record.setVisitId(visitId);
        record.setOrderNo(orderNo);
        record.setAmount(actualDeduct);
        record.setType(AccountConstants.CONSUMPTION_TYPE_VISIT_PAY);
        record.setStatus(AccountConstants.CONSUMPTION_STATUS_SUCCESS);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());

        if (settle != null) {
            record.setSettleId(settle.getId());
        }

        consumptionRecordMapper.insert(record);

        // 更新结算单状态为"患者已支付自付金额"
        if (settle != null) {
            settle.setStatus(ReimburseConstants.SETTLE_STATUS_SELF_PAID);
            settleService.updateById(settle);
            Visit visit = visitService.getById(settle.getVisitId());
            if (visit != null) {
                visit.setStatus(ReimburseConstants.VISIT_STATUS_SETTLED);
                visitService.updateById(visit);
            }
        }

        log.info("支付成功，userId: {}, orderNo: {}, amount: {}, balanceBefore: {}, balanceAfter: {}",
            userId, orderNo, actualDeduct, balanceBefore, balanceAfter);

        return Result.ok("支付成功");
    }

    // 分页查询充值记录（支持时间范围筛选）
    @Override
    public Result rechargeList(Long userId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<RechargeRecord> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getUserId, userId);
        if (startTime != null) {
            wrapper.ge(RechargeRecord::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(RechargeRecord::getCreateTime, endTime);
        }
        wrapper.orderByDesc(RechargeRecord::getCreateTime);

        Page<RechargeRecord> resultPage = rechargeRecordMapper.selectPage(page, wrapper);

        List<RechargeRecordVO> voList = new ArrayList<>();
        for (RechargeRecord record : resultPage.getRecords()) {
            RechargeRecordVO vo = new RechargeRecordVO();
            BeanUtils.copyProperties(record, vo);
            voList.add(vo);
        }

        return Result.ok(voList, resultPage.getTotal());
    }

    // 分页查询消费记录（支持时间范围筛选）
    @Override
    public Result consumptionList(Long userId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<ConsumptionRecord> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<ConsumptionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumptionRecord::getUserId, userId);
        if (startTime != null) {
            wrapper.ge(ConsumptionRecord::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(ConsumptionRecord::getCreateTime, endTime);
        }
        wrapper.orderByDesc(ConsumptionRecord::getCreateTime);

        Page<ConsumptionRecord> resultPage = consumptionRecordMapper.selectPage(page, wrapper);

        List<ConsumptionRecordVO> voList = new ArrayList<>();
        for (ConsumptionRecord record : resultPage.getRecords()) {
            ConsumptionRecordVO vo = new ConsumptionRecordVO();
            BeanUtils.copyProperties(record, vo);
            voList.add(vo);
        }

        return Result.ok(voList, resultPage.getTotal());
    }

    // 查用户账户，不存在则新建
    private UserAccount getOrCreateAccount(Long userId) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);

        if (account == null) {
            return createAccount(userId);
        }

        return account;
    }

    // 创建新账户，初始余额为零
    private UserAccount createAccount(Long userId) {
        UserAccount account = new UserAccount();
        account.setUserId(userId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO);
        account.setTotalConsumption(BigDecimal.ZERO);
        account.setStatus(AccountConstants.ACCOUNT_STATUS_NORMAL);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        this.save(account);
        return account;
    }

    // 生成充值订单号：前缀 + 时间 + 用户ID + 随机数
    private String generateRechargeOrderNo(Long userId) {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String random = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        return AccountConstants.RECHARGE_ORDER_PREFIX + date + userId + random;
    }

    // 生成消费订单号：前缀 + 时间 + 用户ID + 随机数
    private String generateConsumptionOrderNo(Long userId) {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String random = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        return AccountConstants.CONSUMPTION_ORDER_PREFIX + date + userId + random;
    }
}
