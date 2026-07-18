package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 耗材目录表
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("consumable_catalog")
public class ConsumableCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 耗材编码 */
    private String code;

    /** 耗材名称 */
    private String name;

    /** 规格 */
    private String specification;

    /** 甲乙分类：1-甲类 2-乙类 3-自费 */
    private Integer category;

    /** 限额（超出部分自费） */
    private BigDecimal limitAmount;

    /** 备注 */
    private String remark;
}
