package com.winter.cloud.gateway.common.enums;

import lombok.Getter;

/**
 * 响应结果码枚举
 *
 * @author zsq
 */
@Getter
public enum ResultCodeEnum {

    // 通用响应码
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "访问被拒绝"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_TIMEOUT(408, "请求超时"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    
    // 服务器错误 5xx
    INTERNAL_SERVER_ERROR(500, "内部服务器错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // 网关业务错误码 6xxx
    GATEWAY_CONFIG_ERROR(6001, "网关配置错误"),
    GATEWAY_ROUTE_ERROR(6002, "路由配置错误"),
    GATEWAY_FILTER_ERROR(6003, "过滤器执行错误"),
    
    // 认证授权错误码 7xxx
    TOKEN_INVALID(7001, "Token无效"),
    TOKEN_EXPIRED(7002, "Token已过期"),
    TOKEN_MISSING(7003, "Token缺失"),
    PERMISSION_DENIED(7004, "权限不足"),
    
    // 访问控制错误码 8xxx
    IP_NOT_ALLOWED(8001, "IP地址不在白名单中"),
    PATH_NOT_ALLOWED(8002, "路径不在白名单中"),
    USER_NOT_ALLOWED(8003, "用户不在白名单中"),
    ACCESS_FREQUENCY_LIMIT(8004, "访问频率超限"),
    
    // 安全防护错误码 9xxx
    XSS_ATTACK_DETECTED(9001, "检测到XSS攻击"),
    SQL_INJECTION_DETECTED(9002, "检测到SQL注入攻击"),
    MALICIOUS_REQUEST(9003, "恶意请求"),
    SECURITY_VIOLATION(9004, "安全违规"),
    
    // 灰度发布错误码 10xxx
    GRAY_RELEASE_CONFIG_ERROR(10001, "灰度发布配置错误"),
    GRAY_USER_NOT_FOUND(10002, "灰度用户不存在"),
    GRAY_VERSION_ERROR(10003, "灰度版本错误"),
    
    // 负载均衡错误码 11xxx
    LOAD_BALANCER_ERROR(11001, "负载均衡器错误"),
    NO_AVAILABLE_SERVER(11002, "没有可用的服务器"),
    SERVER_HEALTH_CHECK_FAILED(11003, "服务器健康检查失败");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}