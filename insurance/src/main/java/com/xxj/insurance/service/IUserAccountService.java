package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.domain.po.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.RechargeDTO;

import java.time.LocalDateTime;

/**
 * 患者账户服务接口 - 处理患者账户充值、支付、查询等业务
 */
public interface IUserAccountService extends IService<UserAccount> {

    /**
     * 获取用户账户信息
     * 包含余额、充值总额、消费总额
     * @param userId 用户 ID
     * @return 账户信息
     */
    Result getAccount(Long userId);

    /**
     * 患者账户充值
     * 支持微信、支付宝、银行卡、现金四种方式
     * 使用分布式锁防止重复充值
     * @param userId 用户 ID
     * @param dto 充值信息（金额、类型、备注）
     * @return 充值结果
     */
    Result recharge(Long userId, RechargeDTO dto);

    /**
     * 使用账户余额支付就诊自付部分
     * 从结算单获取自付金额，确保金额准确
     * 使用分布式锁防止超额支付
     * @param userId 用户 ID
     * @param visitId 就诊 ID
     * @param remark 备注
     * @return 支付结果
     */
    Result pay(Long userId, Long visitId, String remark);

    /**
     * 查询充值记录列表（分页）
     * @param userId 用户 ID
     * @param pageDTO 分页参数
     * @return 充值记录列表
     */
    Result rechargeList(Long userId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询消费记录列表（分页）
     * @param userId 用户 ID
     * @param pageDTO 分页参数
     * @return 消费记录列表
     */
    Result consumptionList(Long userId, PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime);
}
