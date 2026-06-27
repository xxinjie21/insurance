package com.xxj.insurance.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

// 拦截器：校验 token、从 Redis 获取用户、权限校验，统一返回 JSON 错误
@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    public UserInfoInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Result errorResult = null;

        try {
            // 1. 校验 token
            String token = request.getHeader("token");
            if (StrUtil.isBlank(token)) {
                errorResult = Result.fail(401, "请先登录");
                return false;
            }

            // 2. 从 Redis 获取用户 ID
            String userId = redisTemplate.opsForValue().get("login:token:" + token);
            if (userId == null) {
                errorResult = Result.fail(401, "登录已过期，请重新登录");
                return false;
            }

            // 3. 获取用户角色（提前获取，用于判断 hospitalId 来源）
            String userRoleStr = redisTemplate.opsForValue().get("login:role:" + userId);
            if (StrUtil.isBlank(userRoleStr)) {
                errorResult = Result.fail("无法获取用户角色");
                return false;
            }

            Role userRole = null;
            try {
                userRole = Role.valueOf(Integer.valueOf(userRoleStr));
            } catch (Exception e) {
                log.warn("角色解析失败 userRoleStr:{}", userRoleStr, e);
                errorResult = Result.fail("无效的角色");
                return false;
            }

            // 4. 保存用户信息到 ThreadLocal
            Long uid;
            try {
                uid = Long.valueOf(userId);
            } catch (Exception e) {
                log.warn("用户ID解析失败 userId:{}", userId, e);
                errorResult = Result.fail("无效的用户ID");
                return false;
            }
            String hospitalIdStr = redisTemplate.opsForValue().get("login:hospitalId:" + userId);
            if (StrUtil.isNotBlank(hospitalIdStr)) {
                // 医院角色：使用自身 hospitalId
                try {
                    UserHolder.save(uid, Long.valueOf(hospitalIdStr));
                } catch (Exception e) {
                    log.warn("医院ID解析失败 hospitalIdStr:{}", hospitalIdStr, e);
                    errorResult = Result.fail("无效的医院ID");
                    return false;
                }
            } else if (userRole == Role.ADMIN) {
                // 管理员：检查是否已选择医院（从 Redis 读取管理员选中的医院）
                String adminHospitalId = redisTemplate.opsForValue().get(RedisConstants.ADMIN_SELECTED_HOSPITAL_KEY + userId);
                if (StrUtil.isNotBlank(adminHospitalId)) {
                    try {
                        UserHolder.save(uid, Long.valueOf(adminHospitalId));
                    } catch (Exception e) {
                        log.warn("管理员选中医院ID解析失败 adminHospitalId:{}", adminHospitalId, e);
                        errorResult = Result.fail("无效的医院ID");
                        return false;
                    }
                } else {
                    UserHolder.save(uid);
                }
            } else {
                UserHolder.save(uid);
            }

            // 5. 权限注解校验
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }

            HandlerMethod hm = (HandlerMethod) handler;
            Permission permission = hm.getMethodAnnotation(Permission.class);
            if (permission == null) {
                permission = hm.getBeanType().getAnnotation(Permission.class);
            }
            if (permission == null) {
                return true;
            }

            // 6. 管理员特权
            if (userRole == Role.ADMIN) {
                return true;
            }

            // 7. 校验角色是否匹配
            for (Role requiredRole : permission.value()) {
                if (requiredRole == userRole) {
                    return true;
                }
            }

            errorResult = Result.fail("无权限访问");
            return false;
        } catch (Exception e) {
            log.error("拦截器异常, uri={}", request.getRequestURI(), e);
            errorResult = Result.fail("服务器内部错误");
            return false;
        } finally {
            // 统一写 JSON 格式的错误响应
            if (errorResult != null) {
                writeJsonResponse(response, errorResult);
            }
        }
    }

    // 将 Result 写为 JSON 响应，前端统一处理
    private void writeJsonResponse(HttpServletResponse response, Result result) {
        response.setStatus(200); // HTTP 200，业务错误通过 JSON 的 success 字段判断
        response.setContentType("application/json;charset=UTF-8");
        try {
            PrintWriter writer = response.getWriter();
            writer.write(JSON.toJSONString(result));
            writer.flush();
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }
}
