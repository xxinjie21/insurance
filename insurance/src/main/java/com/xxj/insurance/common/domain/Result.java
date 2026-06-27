package com.xxj.insurance.common.domain;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private Boolean success;      // 成功标志
    private String errorMsg;      // 错误信息
    private Object data;          // 返回数据
    private Long total;           // 分页总条数
    private Integer code;         // 状态码：200 成功，401 未登录等

    public Result() {
    }

    // 成功，无数据
    public static Result ok() {
        Result result = new Result();
        result.setSuccess(true);
        return result;
    }

    // 成功，带返回数据
    public static Result ok(Object data) {
        Result result = new Result();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    // 成功，带分页列表和总数
    public static Result ok(List<?> data, Long total) {
        Result result = new Result();
        result.setSuccess(true);
        result.setData(data);
        result.setTotal(total);
        return result;
    }

    // 失败，仅错误信息
    public static Result fail(String errorMsg) {
        Result result = new Result();
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        return result;
    }

    // 失败，带状态码和错误信息
    public static Result fail(Integer code, String errorMsg) {
        Result result = new Result();
        result.setSuccess(false);
        result.setCode(code);
        result.setErrorMsg(errorMsg);
        return result;
    }
}