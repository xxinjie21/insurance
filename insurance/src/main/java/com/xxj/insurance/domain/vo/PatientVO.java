package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientVO {

    private String name;

    private String idCard;

    private String phone;

    private LocalDateTime createTime;
}
