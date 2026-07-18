package com.xxj.insurance.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prescription")
public class Prescription implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long visitId;
    private Long doctorId;
    /** 0-待审核 1-审核通过 2-审核拒绝 3-已发药 */
    private Integer status;
    private Long pharmacistId;
    private String rejectReason;
    private LocalDateTime createTime;
}
