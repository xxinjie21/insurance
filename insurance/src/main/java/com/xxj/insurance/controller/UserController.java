package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.UserLoginDTO;
import com.xxj.insurance.domain.dto.UserRegisterDTO;
import com.xxj.insurance.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    // 用户登录
    @PostMapping("/login")
    public Result login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO);
    }

    // 用户注册
    @PostMapping("/sign")
    public Result sign(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.sign(userRegisterDTO);
    }

    // 用户登出
    @OperationLog("用户登出")
    @PostMapping("/loginout")
    public Result loginout(@RequestHeader("token") String token) {
        return userService.loginout(token);
    }

    // 搜索患者
    @OperationLog("搜索患者")
    @GetMapping("/search")
    public Result searchPatients(@RequestParam String name) {
        return userService.searchPatients(name);
    }

}
