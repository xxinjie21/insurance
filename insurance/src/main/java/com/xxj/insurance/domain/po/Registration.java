package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("registration")
public class Registration implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long hospitalId;
    private String dept;
    private String doctorName;
    /** 1-普通门诊 2-专家门诊 */
    private Integer regType;
    private BigDecimal regFee;
    /** 0-已挂号 1-已就诊 2-已取消 */
    private Integer status;
    private LocalDateTime createTime;
}
