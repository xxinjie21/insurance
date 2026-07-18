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
 * 就诊表
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("visit")

public class Visit implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    private Long userId;

    
    private Long hospitalId;

    
    private Integer type;

    /**
     * 就诊科室
     */
    private String dept;

    /**
     * 接诊医生姓名
     */
    private String doctorName;

    private String diagnosis;

    
    private Integer status;

    
    private LocalDateTime createTime;


}
