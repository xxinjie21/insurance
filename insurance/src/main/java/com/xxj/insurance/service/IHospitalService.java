package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医院服务接口 - 处理医院注册、查询等业务
 */
public interface IHospitalService extends IService<Hospital> {

    /**
     * 医院注册（签约）
     * 创建医院机构账号
     * @param hospitalDTO 医院信息
     * @return 注册结果
     */
    Result sign(HospitalDTO hospitalDTO);

    /**
     * 医院查询本院患者列表（分页）
     * 根据医院 ID 查询在该医院就诊的患者
     * @param hospitalId 医院 ID
     * @param pageDTO 分页参数
     * @return 患者列表
     */
    Result listMyPatient(Long hospitalId, PageDTO pageDTO);

    /**
     * 审批通过医院
     * @param hospitalId 医院ID
     * @return 操作结果
     */
    Result approveHospital(Long hospitalId);

    /**
     * 拒绝医院注册
     * @param hospitalId 医院ID
     * @return 操作结果
     */
    Result rejectHospital(Long hospitalId);

    Result enableHospital(Long hospitalId);

    Result disableHospital(Long hospitalId);

    /**
     * 分页查询医院列表（支持按名称模糊搜索）
     * @param pageDTO 分页参数（默认第1页，每页10条）
     * @param name 医院名称（可选，模糊搜索）
     * @return 分页结果
     */
    Result listAll(PageDTO pageDTO, String name);

    /**
     * 管理员选择医院
     * 校验医院存在后，将 hospitalId 存入 Redis（有效期24小时）
     * 拦截器会自动将选中的 hospitalId 设入 UserHolder
     * @param hospitalId 医院 ID
     * @return 操作结果
     */
    Result selectHospital(Long hospitalId);

    /**
     * 管理员取消选择的医院
     * 从 Redis 中删除选中的 hospitalId
     * @return 操作结果
     */
    Result unselectHospital();

    /**
     * 管理员获取当前选中的医院信息
     * 从 Redis 读取 hospitalId 后查询医院详情
     * @return 医院信息（可能为 null）
     */
    Result getSelectedHospital();
}
