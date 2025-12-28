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

/**
 * 访问控制过滤器
 * 
 * <p>该过滤器是网关安全防护体系的第一道防线，解决了以下核心问题：</p>
 * <ul>
 *   <li>恶意请求拦截：在请求处理的最早阶段拦截非法访问，防止恶意攻击</li>
 *   <li>资源访问控制：基于路径、IP、用户等多维度实现精细化访问控制</li>
 *   <li>系统资源保护：快速拒绝非法请求，避免后续复杂处理消耗系统资源</li>
 *   <li>安全合规要求：满足企业级安全合规要求，提供访问审计基础</li>
 * </ul>
 * 
 * <p>实现该过滤器带来的具体好处：</p>
 * <ul>
 *   <li>性能优化：最高优先级执行，快速过滤非法请求，减少系统负载</li>
 *   <li>安全防护：多层次白名单机制，有效防范各种网络攻击</li>
 *   <li>灵活配置：支持动态白名单配置，适应不同业务场景需求</li>
 *   <li>运维便利：详细的访问日志记录，便于安全审计和问题排查</li>
 *   <li>扩展性强：模块化设计，易于扩展新的访问控制策略</li>
 * </ul>
 * 
 * <p>与其他组件的调用关系：</p>
 * <ul>
 *   <li>依赖WhitelistService进行白名单验证和管理</li>
 *   <li>作为GlobalFilter在Spring Cloud Gateway过滤器链中最先执行</li>
 *   <li>通过后续的认证过滤器进行更深层次的安全验证</li>
 *   <li>与AuditLogFilter配合记录完整的访问审计信息</li>
 * </ul>
 * 
 * <p>核心业务逻辑：</p>
 * <ol>
 *   <li>IP黑名单检查：如果客户端IP在黑名单中，直接拒绝访问</li>
 *   <li>用户黑名单检查：如果用户在黑名单中，直接拒绝访问</li>
 *   <li>路径黑名单检查：如果请求路径在黑名单中，直接拒绝访问</li>
 *   <li>访问放行：如果所有黑名单检查都通过，放行到下一个过滤器</li>
 *   <li>注意：只要命中任一黑名单条件就拒绝访问，采用"或"逻辑</li>
 * </ol>
 * 
 * @author zsq
 */
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

    /**
     * 过滤器核心处理方法 - 执行访问控制检查
     * 
     * <p>该方法实现了多层次的访问控制策略，采用黑名单机制进行检查：</p>
     * <ol>
     *   <li>IP黑名单：如果IP在黑名单中，直接拒绝</li>
     *   <li>用户黑名单：如果用户在黑名单中，直接拒绝</li>
     *   <li>路径黑名单：如果路径在黑名单中，直接拒绝</li>
     *   <li>访问放行：如果所有黑名单检查都通过，放行到下一个过滤器</li>
     * </ol>
     * 
     * <p>实现原理：</p>
     * <ul>
     *   <li>响应式编程：使用Mono实现非阻塞的异步处理</li>
     *   <li>早期拦截：在过滤器链最前端执行，快速决策</li>
     *   <li>多维度验证：综合路径、IP、用户等多个维度进行判断</li>
     *   <li>灵活放行：只要满足任一白名单条件即可通过</li>
     *   <li>异常处理：完善的异常捕获和错误响应机制</li>
     * </ul>
     * 
     * @param exchange 服务器Web交换对象，包含请求和响应信息
     * @param chain 网关过滤器链，用于传递到下一个过滤器
     * @return Mono<Void> 异步处理结果，表示过滤器处理完成
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // 提取请求关键信息用于访问控制判断
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);
        String userId = request.getHeaders().getFirst(GatewayConstants.Headers.USER_ID);
        
        log.debug("访问控制检查开始: path={}, clientIp={}, userId={}", path, clientIp, userId);

        try {
            // 检查IP黑名单 - 如果IP在黑名单中，直接拒绝
            if (blacklistService.isIpInBlacklist(clientIp)) {
                log.warn("IP黑名单检查失败，拒绝访问: clientIp={}, path={}, userId={}", clientIp, path, userId);
                return handleError(response, ResultCodeEnum.FORBIDDEN);
            }

            // 检查用户黑名单 - 如果用户在黑名单中，直接拒绝
            if (StringUtils.hasText(userId) && blacklistService.isUserInDynamicBlacklist(userId)) {
                log.warn("用户黑名单检查失败，拒绝访问: userId={}, clientIp={}, path={}", userId, clientIp, path);
                return handleError(response, ResultCodeEnum.FORBIDDEN);
            }

            // 检查路径黑名单 - 如果路径在黑名单中，直接拒绝
            if (blacklistService.isPathInBlacklist(path)) {
                log.warn("路径黑名单检查失败，拒绝访问: path={}, clientIp={}, userId={}", path, clientIp, userId);
                return handleError(response, ResultCodeEnum.FORBIDDEN);
            }

            // 所有黑名单检查都通过，放行到下一个过滤器
            log.debug("访问控制检查通过: path={}, clientIp={}, userId={}", path, clientIp, userId);
            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("访问控制检查时发生错误: path={}, clientIp={}, userId={}", path, clientIp, userId, e);
            return handleError(response, ResultCodeEnum.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 
     * <p>该方法通过多种HTTP头信息获取客户端真实IP，解决了以下问题：</p>
     * <ul>
     *   <li>代理服务器IP伪装：穿透各种代理获取真实客户端IP</li>
     *   <li>负载均衡器转发：处理负载均衡器添加的转发头信息</li>
     *   <li>CDN加速服务：识别CDN服务商添加的真实IP头</li>
     * </ul>
     * 
     * <p>实现原理：</p>
     * <ul>
     *   <li>优先级检查：按照常见代理头的可信度依次检查</li>
     *   <li>格式验证：验证IP地址格式的合法性</li>
     *   <li>多IP处理：处理X-Forwarded-For中的多IP情况</li>
     *   <li>兜底机制：最终从Socket连接获取远程地址</li>
     * </ul>
     * 
     * @param request HTTP请求对象，包含各种头信息
     * @return String 客户端真实IP地址，获取失败时返回"unknown"
     */
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

    /**
     * 验证IP地址格式的合法性
     * 
     * <p>该方法实现了基础的IPv4地址格式验证，确保IP地址的有效性：</p>
     * <ul>
     *   <li>格式检查：验证是否符合IPv4的四段式格式</li>
     *   <li>数值范围：确保每段数值在0-255的有效范围内</li>
     *   <li>异常处理：捕获数字格式异常，防止程序崩溃</li>
     * </ul>
     * 
     * @param ip 待验证的IP地址字符串
     * @return boolean true表示IP格式有效，false表示格式无效
     */
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