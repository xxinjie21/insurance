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
 * 诊疗项目目录表
 *
 * @author xxj
 * @since 2026-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("treatment_catalog")
public class TreatmentCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 诊疗项目编码 */
    private String code;

    /** 项目名称 */
    private String name;

    /** 项目类别：检查/检验/手术/治疗/护理/诊查 */
    private String projectType;

    /** 甲乙分类：1-甲类 2-乙类 3-自费 */
    private Integer category;

    /** 单价上限 */
    private BigDecimal unitPriceCap;

    /** 备注 */
    private String remark;
}
