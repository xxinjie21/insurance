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
 * 药品目录表
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("drug_catalog")
public class DrugCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 医保药品编码 */
    private String code;

    /** 药品通用名 */
    private String name;

    /** 规格 */
    private String specification;

    /** 生产厂家 */
    private String manufacturer;

    /** 甲乙分类：1-甲类 2-乙类 3-自费 */
    private Integer category;

    /** 乙类自付比例 */
    private BigDecimal selfPayRatio;

    /** 备注 */
    private String remark;
}
