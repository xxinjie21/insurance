package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表：患者/医院/医保局/管理员
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "user", autoResultMap = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String password;

    private String name;

    private String phone;

    @TableField("id_card")
    private String idCard;

    private Long hospitalId;

    private Integer role;

    /**
     * 参保类型：1-职工 2-居民(仅患者)
     */
    private Integer insuranceType;

    /**
     * 医保个人编号(仅患者)
     */
    private String insuranceNo;

    /**
     * 参保地(仅患者)
     */
    private String insuranceCity;

    /**
     * 医保个人账户余额(仅职工参保患者)
     */
    private java.math.BigDecimal personalAccountBalance;

    /** 医疗救助标记：0-否 1-是(低保/特困) */
    private Integer medicalAssistance;

    private LocalDateTime createTime;


}
