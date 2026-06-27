package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.vo.PatientVO;
import com.xxj.insurance.mapper.HospitalMapper;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.IHospitalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 医院表 服务实现类
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, Hospital> implements IHospitalService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    // 医院注册：校验名称/手机号唯一性 → 建医院 → 返回ID
    @Override
    public Result sign(HospitalDTO hospitalDTO) {
        // 1. 获取名称
        String name = hospitalDTO.getName();

        // 2. 校验医院名称唯一性
        LambdaQueryWrapper<Hospital> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Hospital::getName, name);
        if (this.count(nameWrapper) > 0) {
            return Result.fail("该医院名称已存在");
        }

        // 2.1 校验手机号唯一性
        LambdaQueryWrapper<Hospital> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(Hospital::getPhone, hospitalDTO.getPhone());
        if (this.count(phoneWrapper) > 0) {
            return Result.fail("该手机号已被注册");
        }

        // 3. 新建医院对象
        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setAddress(hospitalDTO.getAddress());
        hospital.setPhone(hospitalDTO.getPhone());
        hospital.setPassword(passwordEncoder.encode(hospitalDTO.getPassword()));
        hospital.setStatus(0); // 默认待审批

        // 3. 保存（MyBatis-Plus 保存后自动回写 ID 到对象里）
        save(hospital);

        // 4. 直接获取保存后的 ID
        Long hospitalId = hospital.getId();

        // 5. 返回 ID 给前端
        return Result.ok(hospitalId);
    }


    // 分页查询该医院下的患者列表
    @Override
    public Result listMyPatient(Long hospitalId, PageDTO pageDTO) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        Page<User> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, Role.PATIENT.getCode())
                .eq(User::getHospitalId, hospitalId);

        Page<User> userPage = userMapper.selectPage(page, wrapper);

        List<PatientVO> records = userPage.getRecords().stream().map(user -> {
            PatientVO vo = new PatientVO();
            vo.setName(user.getName());
            vo.setIdCard(maskIdCard(user.getIdCard()));
            vo.setPhone(maskPhone(user.getPhone()));
            vo.setCreateTime(user.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        PageDTO<PatientVO> result = new PageDTO<>(pageDTO.getPageNum(), pageDTO.getPageSize(), userPage.getTotal(), records);
        return Result.ok(result);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.replaceAll("(.{6}).*(.{4})", "$1****$2");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.replaceAll("(.{3}).*(.{4})", "$1****$2");
    }

    // 审批通过医院
    @Override
    public Result approveHospital(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院ID不能为空");
        }
        Hospital hospital = getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }
        if (hospital.getStatus() != 0) {
            return Result.fail("只能审批待审批状态的医院");
        }
        hospital.setStatus(1);
        updateById(hospital);
        return Result.ok("审批通过");
    }

    // 拒绝医院注册
    @Override
    public Result rejectHospital(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院ID不能为空");
        }
        Hospital hospital = getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }
        if (hospital.getStatus() != 0) {
            return Result.fail("只能拒绝待审批状态的医院");
        }
        hospital.setStatus(3);
        updateById(hospital);
        return Result.ok("已拒绝");
    }

    // 启用医院
    @Override
    public Result enableHospital(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院ID不能为空");
        }
        Hospital hospital = getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }
        if (hospital.getStatus() != 2) {
            return Result.fail("只能启用已停用状态的医院");
        }
        hospital.setStatus(1);
        updateById(hospital);
        return Result.ok("启用成功");
    }

    // 停用医院
    @Override
    public Result disableHospital(Long hospitalId) {
        if (hospitalId == null) {
            return Result.fail("医院ID不能为空");
        }
        Hospital hospital = getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }
        if (hospital.getStatus() != 1) {
            return Result.fail("只能停用已启用状态的医院");
        }
        hospital.setStatus(2);
        updateById(hospital);
        return Result.ok("停用成功");
    }

    // 分页查询医院列表，支持按名称模糊搜索
    @Override
    public Result listAll(PageDTO pageDTO, String name) {
        // 1. 处理分页参数默认值
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }

        // 2. 构建分页对象
        Page<Hospital> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        // 3. 构建查询条件：按名称模糊搜索 + ID 升序
        LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Hospital::getName, name);
        }
        wrapper.orderByAsc(Hospital::getId);

        // 4. 执行分页查询
        Page<Hospital> hospitalPage = this.page(page, wrapper);
        return Result.ok(hospitalPage);
    }

    // 管理员选择医院：校验存在后存入 Redis，有效期 24 小时
    @Override
    public Result selectHospital(Long hospitalId) {
        // 1. 校验医院是否存在
        Hospital hospital = this.getById(hospitalId);
        if (hospital == null) {
            return Result.fail("医院不存在");
        }

        // 2. 获取当前管理员 ID，将选中的医院存入 Redis
        Long userId = UserHolder.getUserId();
        redisTemplate.opsForValue().set(
                RedisConstants.ADMIN_SELECTED_HOSPITAL_KEY + userId,
                String.valueOf(hospitalId),
                RedisConstants.ADMIN_SELECTED_HOSPITAL_TTL,
                TimeUnit.SECONDS
        );
        return Result.ok(hospitalId);
    }

    // 管理员取消选择的医院：从 Redis 中删除
    @Override
    public Result unselectHospital() {
        Long userId = UserHolder.getUserId();
        redisTemplate.delete(RedisConstants.ADMIN_SELECTED_HOSPITAL_KEY + userId);
        return Result.ok(null);
    }

    // 管理员获取当前选中的医院：从 Redis 读取并查询医院详情
    @Override
    public Result getSelectedHospital() {
        // 1. 从 Redis 读取管理员选中的 hospitalId
        Long userId = UserHolder.getUserId();
        String hospitalId = redisTemplate.opsForValue().get(
                RedisConstants.ADMIN_SELECTED_HOSPITAL_KEY + userId);

        // 2. 如果选中了医院，查询并返回医院详情
        if (hospitalId != null) {
            Hospital hospital = this.getById(Long.valueOf(hospitalId));
            return Result.ok(hospital);
        }
        return Result.ok(null);
    }

}
