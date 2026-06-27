package com.xxj.insurance.common.domain;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 分页结果封装
 * 包含分页参数和数据列表
 */
@Data
public class PageDTO<T> {
    // 当前页码
    @Min(value = 1, message = "页码最小为1")
    Integer pageNum = 1;
    // 每页大小
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    Integer pageSize = 10;
    // 总记录数
    Long total = 0L;
    // 当前页数据列表
    List<T> records;
    
    // 无参构造函数
    public PageDTO() {
    }
    
    // 有参构造函数
    public PageDTO(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
    
    // 完整参数构造函数
    public PageDTO(Integer pageNum, Integer pageSize, Long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
    }
}
