package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.JwtUtil;
import com.xxj.insurance.domain.dto.UserLoginDTO;
import com.xxj.insurance.domain.dto.UserRegisterDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.vo.UserLoginVO;
import com.xxj.insurance.domain.vo.UserRegisterVO;
import com.xxj.insurance.mapper.HospitalMapper;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final HospitalMapper hospitalMapper;

    // 用户登录：校验 → 查用户 → 兼容旧MD5 → 发token → 缓存角色
    @Override
    public Result login(UserLoginDTO userLoginDTO) {
        String phone = userLoginDTO.getPhone();
        String password = userLoginDTO.getPassword();
        Integer role = userLoginDTO.getRole();

        // 1. 校验
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(password) || role == null) {
            return Result.fail("手机号、密码、角色不能为空");
        }

        // 2. 根据手机号查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = this.getOne(wrapper);

        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 3. 校验角色
        if (!user.getRole().equals(role)) {
            return Result.fail("角色不匹配");
        }

        // 4. 校验密码
        // 系统从MD5迁移至BCrypt，旧用户密码仍是MD5，登录成功时自动升级
        // 兼容旧 MD5 密码：登录成功后自动升级为 BCrypt
        String dbPassword = user.getPassword();
        if (isBcryptPassword(dbPassword)) {
            // BCrypt 密码，直接匹配
            if (!passwordEncoder.matches(password, dbPassword)) {
                return Result.fail("密码错误");
            }
        } else {
            // 旧 MD5 密码，MD5 匹配后自动升级
            String md5Password = DigestUtil.md5Hex(password);
            if (!dbPassword.equals(md5Password)) {
                return Result.fail("密码错误");
            }
            // 自动升级为 BCrypt
            user.setPassword(passwordEncoder.encode(password));
            this.updateById(user);
            log.info("用户{}密码已从MD5自动升级为BCrypt", user.getId());
        }

        // 5. 生成 token（包含 userId）
        String token = jwtUtil.createToken(user.getId());

        // 6. 存入 Redis
        redisTemplate.opsForValue().set("login:token:" + token, user.getId().toString(), RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("login:role:" + user.getId(), user.getRole().toString(), RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 7. 如果是医院角色，保存 hospitalId
        if (Role.HOSPITAL.getCode().equals(user.getRole()) && user.getHospitalId() != null) {
            redisTemplate.opsForValue().set("login:hospitalId:" + user.getId(), user.getHospitalId().toString(), RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        }

        // 8. 返回（身份证号脱敏）
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setHospitalId(user.getHospitalId());
        // 查询医院名称
        if (user.getHospitalId() != null) {
            Hospital hospital = hospitalMapper.selectById(user.getHospitalId());
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }
        }
        vo.setToken(token);
        vo.setIdCard(maskIdCard(user.getIdCard()));

        return Result.ok(vo);
    }

    // 用户注册：参数校验 → 手机号/身份证查重 → 医院验证 → 建用户
    @Override
    public Result sign(UserRegisterDTO userRegisterDTO) {
        String phone = userRegisterDTO.getPhone();
        String password = userRegisterDTO.getPassword();
        String name = userRegisterDTO.getName();
        String idCard = userRegisterDTO.getIdCard();
        String hospitalPhone = userRegisterDTO.getHospitalPhone();
        String hospitalPassword = userRegisterDTO.getHospitalPassword();
        Integer role = userRegisterDTO.getRole();
        Long hospitalId = null;

        // 基础校验
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(name) || StrUtil.isBlank(password) || role == null) {
            return Result.fail("手机号、姓名、密码、角色不能为空");
        }

        // 校验手机号唯一性
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, phone);
        if (this.count(phoneWrapper) > 0) {
            return Result.fail("该手机号已注册");
        }

        // 所有角色都必须填身份证号
        if (StrUtil.isBlank(idCard)) {
            return Result.fail("身份证号不能为空");
        }

        // 身份证号与报销业务绑定，一人一号不可重复
        // 如果填了身份证号，校验唯一性
        if (StrUtil.isNotBlank(idCard)) {
            LambdaQueryWrapper<User> idCardWrapper = new LambdaQueryWrapper<>();
            idCardWrapper.eq(User::getIdCard, idCard);
            if (this.count(idCardWrapper) > 0) {
                return Result.fail("该身份证号已注册");
            }
        }

        // 医院角色必须提供医院注册手机号和密码进行验证
        if (role == 2) {
            if (StrUtil.isBlank(hospitalPhone) || StrUtil.isBlank(hospitalPassword)) {
                return Result.fail("医院角色必须填写医院注册手机号和密码");
            }
            LambdaQueryWrapper<Hospital> hospitalWrapper = new LambdaQueryWrapper<>();
            hospitalWrapper.eq(Hospital::getPhone, hospitalPhone);
            Hospital hospital = hospitalMapper.selectOne(hospitalWrapper);
            if (hospital == null) {
                return Result.fail("医院不存在，请检查手机号或在医保局注册医院");
            }
            // 验证医院密码，确保注册者属于该医院，防止冒名
            if (!passwordEncoder.matches(hospitalPassword, hospital.getPassword())) {
                return Result.fail("医院密码错误");
            }
            if (hospital.getStatus() != 1) {
                return Result.fail("该医院尚未通过审批，无法注册");
            }
            hospitalId = hospital.getId();
        }

        // 构建用户（BCrypt 加密密码）
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setRole(role);
        user.setCreateTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(idCard)) {
            user.setIdCard(idCard);
        }
        if (hospitalId != null) {
            user.setHospitalId(hospitalId);
        }

        save(user);

        // 返回 VO（不包含 id、密码、身份证号）
        UserRegisterVO vo = new UserRegisterVO();
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return Result.ok(vo);
    }

    // 登出：删除 Redis 中对应的 token
    @Override
    public Result loginout(String token) {
        redisTemplate.delete("login:token:" + token);
        return Result.ok("登出成功");
    }

    /**
     * 判断密码是否为 BCrypt 格式
     * BCrypt 密码以 $2a$ 或 $2b$ 开头，长度 60
     */
    private boolean isBcryptPassword(String password) {
        return password != null && password.startsWith("$2") && password.length() == 60;
    }

    /**
     * 身份证号脱敏：保留前4位和后4位，中间用 **** 替代
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return "***";
        }
        return idCard.substring(0, 4) + "****" + idCard.substring(idCard.length() - 4);
    }

    // 按姓名模糊搜索患者（身份证脱敏后返回）
    @Override
    public Result searchPatients(String name) {
        if (StrUtil.isBlank(name)) {
            return Result.fail("姓名不能为空");
        }
        List<User> users = lambdaQuery()
                .eq(User::getRole, 1)
                .like(User::getName, name)
                .list();
        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("idCard", maskIdCard(user.getIdCard()));
            return map;
        }).collect(toList());
        return Result.ok(result);
    }

    // 异步将患者绑定到指定医院（就诊结算后自动关联）
    @Async("visitAsyncExecutor")
    @Override
    public void asyncBindHospital(Long userId, Long hospitalId) {
        if (userId == null || hospitalId == null) {
            log.warn("异步绑定医院参数为空: userId={}, hospitalId={}", userId, hospitalId);
            return;
        }
        try {
            User user = getById(userId);
            if (user == null) {
                log.warn("异步绑定医院失败：用户不存在, userId={}", userId);
                return;
            }
            // 已经绑定到该医院，无需重复操作
            if (hospitalId.equals(user.getHospitalId())) {
                log.debug("患者(userId={})已属于医院(hospitalId={})，跳过绑定", userId, hospitalId);
                return;
            }
            // 绑定到医院
            user.setHospitalId(hospitalId);
            updateById(user);
            log.info("异步绑定成功：患者(userId={})已加入医院(hospitalId={})", userId, hospitalId);
        } catch (Exception e) {
            log.error("异步绑定医院异常：userId={}, hospitalId={}", userId, hospitalId, e);
        }
    }
}
