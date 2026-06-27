package com.xxj.insurance.common.enums;

/**
 * 用户角色枚举
 * 与数据库 user.role 字段对应：1-患者 2-医院 3-医保局 4-管理员
 */
public enum Role {
    PATIENT(1, "患者"),
    HOSPITAL(2, "医院"),
    MEDICAL(3, "医保局"),
    ADMIN(4, "管理员");

    private final Integer code;  // 角色编码
    private final String name;   // 角色名称

    Role(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    // 根据编码查找角色
    public static Role valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}