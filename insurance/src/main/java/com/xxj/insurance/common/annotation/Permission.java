package com.xxj.insurance.common.annotation;

import com.xxj.insurance.common.enums.Role;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    Role[] value();
}
