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
    UNAUTHENTICATED("0009", "未认证"),
    UNAUTHORIZED("0006", "未授权"),
    REQUEST_PARAMETER_ERROR("412", "请求参数异常"),
    BODY_PARAMETER_ERROR("413", "请求体异常"),
    METHOD_ERROR("414", "请求方式不支持"),



    SUCCESS_LANG("200", "result.code.success"),
    FAIL_LANG("500", "result.code.fail"),
    UNAUTHENTICATED_LANG("0009", "result.code.invalid.token"),
    UNAUTHORIZED_LANG("0006", "result.code.no.operator.auth"),
    REQUEST_PARAMETER_ERROR_LANG("412", "result.code.request.parameter.error"),
    BODY_PARAMETER_ERROR_LANG("413", "result.code.body.parameter.error"),
    METHOD_ERROR_LANG("414", "result.code.method.error"),


    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    LOGIN_FAILED("0003", "用户名或密码错误"),
    DUPLICATE_KEY("0004", "数据已存在"),
    NOT_FOUND("0005", "数据不存在"),
    FORBIDDEN("0007", "禁止访问"),
    DISABLED("0008", "禁用"),


    ;

    private String code;
    private String message;

}
