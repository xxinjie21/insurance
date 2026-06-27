package com.xxj.insurance.service.impl;

import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.UserAccountVO;
import com.xxj.insurance.service.IBatchService;
import com.xxj.insurance.service.IDashboardService;
import com.xxj.insurance.service.IUserAccountService;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final IVisitService visitService;
    private final IUserAccountService userAccountService;
    private final IBatchService batchService;

    // 获取首页统计：不同角色展示不同维度的数据
    @Override
    public Result getStats(Integer role) {
        Map<String, Object> stats = new HashMap<>();

        // 患者端：就诊次数 + 账户余额
        if (Role.PATIENT.getCode().equals(role)) {
            Long userId = UserHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            long visitCount = visitService.lambdaQuery()
                    .eq(Visit::getUserId, userId)
                    .count();
            stats.put("visitCount", visitCount);

            Result accountResult = userAccountService.getAccount(userId);
            if (accountResult.getSuccess() && accountResult.getData() instanceof UserAccountVO) {
                UserAccountVO account = (UserAccountVO) accountResult.getData();
                stats.put("balance", account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
            } else {
                stats.put("balance", BigDecimal.ZERO);
            }
            // 医院端：待处理就诊数 + 待提交批次 + 待拨付总额
        } else if (Role.HOSPITAL.getCode().equals(role)) {
            Long hospitalId = UserHolder.getHospitalId();
            if (hospitalId == null) {
                return Result.fail("请先选择医院");
            }
            long pendingVisitCount = visitService.lambdaQuery()
                    .eq(Visit::getHospitalId, hospitalId)
                    .eq(Visit::getStatus, ReimburseConstants.VISIT_STATUS_PENDING)
                    .count();
            stats.put("pendingVisitCount", pendingVisitCount);

            long pendingBatchCount = batchService.lambdaQuery()
                    .eq(Batch::getHospitalId, hospitalId)
                    .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_PENDING)
                    .count();
            stats.put("pendingBatchCount", pendingBatchCount);

            List<Batch> declaredBatches = batchService.lambdaQuery()
                    .eq(Batch::getHospitalId, hospitalId)
                    .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_DECLARED)
                    .list();
            BigDecimal pendingDisburse = declaredBatches.stream()
                    .map(Batch::getTotalAmt)
                    .filter(amt -> amt != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("pendingDisburseAmount", pendingDisburse);
            // 医保端：待审核批次 + 待拨付总额 + 已拨付批次
        } else if (Role.MEDICAL.getCode().equals(role)) {
            long pendingReviewBatchCount = batchService.lambdaQuery()
                    .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_DECLARED)
                    .count();
            stats.put("pendingReviewBatchCount", pendingReviewBatchCount);

            // 已申报但未拨付的批次，汇总待拨付金额
            List<Batch> declaredBatches = batchService.lambdaQuery()
                    .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_DECLARED)
                    .list();
            BigDecimal pendingDisburse = declaredBatches.stream()
                    .map(Batch::getTotalAmt)
                    .filter(amt -> amt != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("pendingDisburseAmount", pendingDisburse);

            long disbursedBatchCount = batchService.lambdaQuery()
                    .eq(Batch::getStatus, ReimburseConstants.BATCH_STATUS_COMPLETED)
                    .count();
            stats.put("disbursedBatchCount", disbursedBatchCount);
        }

        return Result.ok(stats);
    }
}
