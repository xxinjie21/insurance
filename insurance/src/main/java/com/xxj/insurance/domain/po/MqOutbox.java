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
@TableName("mq_outbox")
public class MqOutbox implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String messageId;
    private String exchange;
    private String routingKey;
    private String payload;
    /** 0-待发送 1-已发送 2-发送失败 */
    private Integer status;
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
