package com.xxj.insurance.common.aspect;

import com.alibaba.fastjson.JSON;
import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Long userId = UserHolder.getUserId();
        String operation = operationLog.value();
        String method = joinPoint.getSignature().toShortString();
        String argsJson = JSON.toJSONString(Arrays.stream(joinPoint.getArgs())
                .filter(a -> !(a instanceof javax.servlet.http.HttpServletRequest)
                        && !(a instanceof javax.servlet.http.HttpServletResponse)).toArray());

        long start = System.currentTimeMillis();
        String resultSummary = "成功";
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            persistLog(userId, operation, method, argsJson, resultSummary, elapsed);
            log.info("【操作日志】用户:{} 操作:{} 耗时:{}ms", userId, operation, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            resultSummary = "失败:" + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(100, e.getMessage().length())) : "未知");
            persistLog(userId, operation, method, argsJson, resultSummary, elapsed);
            log.error("【操作日志】用户:{} 操作:{} 失败:{} 耗时:{}ms", userId, operation, e.getMessage(), elapsed);
            throw e;
        }
    }

    private void persistLog(Long userId, String operation, String method, String params, String result, long duration) {
        try {
            com.xxj.insurance.domain.po.OperationLog po = new com.xxj.insurance.domain.po.OperationLog();
            po.setUserId(userId);
            po.setOperation(operation);
            po.setMethod(method);
            po.setParams(params != null && params.length() > 500 ? params.substring(0, 500) : params);
            po.setResult(result != null && result.length() > 200 ? result.substring(0, 200) : result);
            po.setDurationMs(duration);
            po.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(po);
        } catch (Exception e) {
            log.warn("操作日志入库失败: {}", e.getMessage());
        }
    }
}
