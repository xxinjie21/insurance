package com.xxj.insurance.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", message = "身份证号格式不正确")
    private String idCard;

    private String hospitalPhone;

    private String hospitalPassword;

    @NotNull(message = "角色不能为空")
    private Integer role;

    /** 参保类型：1-职工 2-居民（仅患者角色有效） */
    private Integer insuranceType;

    /** 参保地 */
    private String insuranceCity;
}
