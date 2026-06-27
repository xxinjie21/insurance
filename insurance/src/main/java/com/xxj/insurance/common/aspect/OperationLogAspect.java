package com.xxj.insurance.common.aspect;

import com.alibaba.fastjson.JSON;
import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Long userId = UserHolder.getUserId();
        String operation = operationLog.value();
        String argsJson = JSON.toJSONString(joinPoint.getArgs());

        log.info("【操作日志】用户ID：{}，操作：{}，参数：{}", userId, operation, argsJson);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("【操作日志】用户ID：{}，操作：{}，结果：成功，耗时：{}ms", userId, operation, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("【操作日志】用户ID：{}，操作：{}，结果：失败，原因：{}，耗时：{}ms", userId, operation, e.getMessage(), elapsed);
            throw e;
        }
    }
}
