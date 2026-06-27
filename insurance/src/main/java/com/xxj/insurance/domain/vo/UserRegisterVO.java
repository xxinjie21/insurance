package com.xxj.insurance.domain.vo;



import lombok.Data;

import java.time.LocalDateTime;

@Data

public class UserRegisterVO {


    private String name;


    private Integer role;


    private LocalDateTime createTime;
}
