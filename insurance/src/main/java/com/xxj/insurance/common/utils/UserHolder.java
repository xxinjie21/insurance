package com.xxj.insurance.common.utils;

import com.xxj.insurance.common.exception.BusinessException;

// 请求线程中保存当前登录用户信息
public class UserHolder {

    // 存储 用户 ID
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();
    // 存储 医院 ID（医院角色自身ID，或管理员选中的医院ID）
    private static final ThreadLocal<Long> HOSPITAL_TL = new ThreadLocal<>();

    // 保存用户 ID
    public static void save(Long userId) {
        TL.set(userId);
    }

    // 保存用户 ID 和医院 ID
    public static void save(Long userId, Long hospitalId) {
        TL.set(userId);
        HOSPITAL_TL.set(hospitalId);
    }

    // 获取当前用户 ID
    public static Long getUserId() {
        return TL.get();
    }

    // 获取医院 ID，未选择则返回 null
    public static Long getHospitalId() {
        return HOSPITAL_TL.get();
    }

    // 获取医院 ID，未选择时抛异常
    public static Long requireHospitalId() {
        Long hospitalId = HOSPITAL_TL.get();
        if (hospitalId == null) {
            throw new BusinessException("请先选择医院");
        }
        return hospitalId;
    }

    // 移除用户信息，防止内存泄漏
    public static void remove() {
        TL.remove();
        HOSPITAL_TL.remove();
    }
}
