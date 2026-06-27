package com.xxj.insurance.domain.dto;



import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * @ClassName HospitalDTO
 * @Description
 * @Author xxinj
 * @Date 2026/4/21 15:46
 * @Version 1.0
 */

@Data

public class HospitalDTO {

    @NotBlank(message = "医院名称不能为空")
    private String name;

    private String address;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
}