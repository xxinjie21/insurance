package com.xxj.insurance.domain.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private Long userId;
    private String name;
    private Integer role;
    private Long hospitalId;
    private String hospitalName; // 医院名称，登录时查询填充
    private String token;
    private String idCard;  // 脱敏后的身份证号（如 3201****1234）
}
