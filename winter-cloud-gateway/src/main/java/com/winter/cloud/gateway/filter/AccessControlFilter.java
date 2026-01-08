package com.winter.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.gateway.common.entity.Result;
import com.winter.cloud.gateway.common.enums.ResultCodeEnum;
import com.winter.cloud.gateway.constants.GatewayConstants;
import com.winter.cloud.gateway.service.BlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
@Slf4j
@Component
public class AccessControlFilter implements GlobalFilter, Ordered {
    /**
     * 白名单服务，提供多维度白名单验证功能
     */
    private final BlacklistService blacklistService;

    /**
     * JSON对象映射器，用于序列化响应数据
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数 - 注入白名单服务依赖
     * 
     * @param blacklistService 白名单服务实例，用于执行各种白名单验证
     * @param objectMapper JSON对象映射器，用于序列化响应数据
     */
    public AccessControlFilter(BlacklistService blacklistService, ObjectMapper objectMapper) {
        this.blacklistService = blacklistService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 放行OPTIONS请求
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        String clientIp = getClientIp(request);


        try {
            // 检查IP黑名单 - 如果IP在黑名单中，直接拒绝
            if (blacklistService.isIpInBlacklist(clientIp)) {
                log.warn("IP黑名单检查失败，拒绝访问: clientIp={}", clientIp);
                return handleError(response, ResultCodeEnum.FORBIDDEN);
            }


            // 所有黑名单检查都通过，放行到下一个过滤器
            log.debug("访问控制检查通过: clientIp={}", clientIp);
            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("访问控制检查时发生错误:, clientIp={}，异常信息={}", clientIp, e);
            return handleError(response, ResultCodeEnum.INTERNAL_SERVER_ERROR);
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        // 尝试从各种头信息中获取真实IP，按可信度排序
        String[] ipHeaders = {
            GatewayConstants.Headers.X_FORWARDED_FOR,      // 最常用的代理转发头
            GatewayConstants.Headers.X_REAL_IP,            // Nginx等反向代理常用
            GatewayConstants.Headers.PROXY_CLIENT_IP,      // Apache代理服务器
            GatewayConstants.Headers.WL_PROXY_CLIENT_IP,   // WebLogic代理服务器
            GatewayConstants.Headers.HTTP_CLIENT_IP,       // 某些代理服务器
            GatewayConstants.Headers.HTTP_X_FORWARDED_FOR  // 标准HTTP转发头
        };

        for (String header : ipHeaders) {
            String ip = request.getHeaders().getFirst(header);
            if (StringUtils.hasText(ip) && !GatewayConstants.Common.UNKNOWN.equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，格式：client, proxy1, proxy2
                // 取第一个IP作为真实客户端IP
                if (ip.contains(GatewayConstants.Common.COMMA)) {
                    ip = ip.split(GatewayConstants.Common.COMMA)[0].trim();
                }
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }

        // 如果所有代理头都没有有效IP，从Socket连接获取远程地址
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return GatewayConstants.Common.UNKNOWN;
    }

    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        // 简单的IPv4格式验证：xxx.xxx.xxx.xxx
        String[] parts = ip.split("\\.");
        if (parts.length != GatewayConstants.Security.IP_SEGMENT_COUNT) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                // 每段数值必须在0-255范围内
                if (num < GatewayConstants.Security.IP_SEGMENT_MIN || num > GatewayConstants.Security.IP_SEGMENT_MAX) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            // 包含非数字字符，格式无效
            return false;
        }
    }

    /**
     * 处理错误响应
     * 
     * <p>该方法统一处理访问控制过程中的各种错误情况，提供标准化的错误响应：</p>
     * <ul>
     *   <li>响应格式统一：使用Result实体类提供标准错误响应</li>
     *   <li>状态码设置：根据错误类型设置合适的HTTP状态码</li>
     *   <li>错误信息：提供清晰的错误描述信息</li>
     *   <li>时间戳记录：添加错误发生时间，便于问题追踪</li>
     * </ul>
     * 
     * @param response HTTP响应对象，用于设置响应状态和内容
     * @param resultCode 结果码枚举，表示错误类型
     * @return Mono<Void> 异步响应结果，表示错误处理完成
     */
    private Mono<Void> handleError(ServerHttpResponse response, ResultCodeEnum resultCode) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        try {
            // 使用统一的Result实体类构造错误响应
            Result<Void> result = Result.fail(resultCode);
            String body = objectMapper.writeValueAsString(result);
            
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("序列化错误响应失败", e);
            // 如果序列化失败，返回简单的错误信息
            String fallbackBody = "{\"success\":false,\"code\":500,\"message\":\"系统内部错误\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * 获取过滤器执行顺序
     * 
     * <p>返回最高优先级，确保访问控制在所有其他过滤器之前执行：</p>
     * <ul>
     *   <li>安全优先：在任何业务逻辑处理前进行安全检查</li>
     *   <li>性能优化：尽早拦截非法请求，减少资源消耗</li>
     *   <li>架构清晰：明确的执行顺序，便于理解和维护</li>
     * </ul>
     * 
     * @return int 过滤器执行顺序，数值越小优先级越高
     */
    @Override
    public int getOrder() {
        // 设置为最高优先级，在所有其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}