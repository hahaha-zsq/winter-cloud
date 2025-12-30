package com.winter.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResultCodeEnum {

    SUCCESS("200", "成功"),
    FAIL("500", "失败"),
    REQUEST_PARAMETER_ERROR("412", "请求参数异常"),
    BODY_PARAMETER_ERROR("413", "请求体异常"),
    METHOD_ERROR("414", "请求方式不支持"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    LOGIN_FAILED("0003", "用户名或密码错误"),
    DUPLICATE_KEY("0004", "数据已存在"),
    NOT_FOUND("0005", "数据不存在"),
    UNAUTHORIZED("0006", "未授权"),
    FORBIDDEN("0007", "禁止访问"),
    DISABLED("0008", "禁用"),


    ;

    private String code;
    private String message;

}
