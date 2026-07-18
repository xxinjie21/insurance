package com.xxj.insurance.domain.po;

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
 * 医院表
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hospital")
public class Hospital implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    private String phone;

    private String password;

    /**
     * 状态：0-待审批 1-已启用 2-已停用 3-已拒绝
     */
    private Integer status;

    /**
     * 医院等级：1-三甲 2-三乙 3-二甲 4-二乙 5-一级 6-社区
     */
    private Integer level;

    /**
     * 定点协议有效期
     */
    private java.time.LocalDate agreementExpire;

    private LocalDateTime createTime;


}
