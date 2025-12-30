package com.winter.cloud.gateway.filter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.util.JwtUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.winter.cloud.common.constants.CommonConstants.buildUserCacheKey;
/**
 * 全局认证过滤器
 * <p>
 * 作用：
 * 1. 拦截所有经过 Gateway 的请求
 * 2. 从请求头中解析 JWT Token
 * 3. 校验 Token 合法性与有效性
 * 4. 通过 Redis + 远程认证服务获取用户信息
 * 5. 将用户信息写入请求 Header，透传给下游服务
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    /** 请求头中 Authorization 的 key */
    private static final String AUTHORIZATION_HEADER = CommonConstants.Headers.AUTHORIZATION;

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = CommonConstants.Headers.BEARER_PREFIX;

    /** 下游服务可获取的用户信息 Header */
    private static final String USER_ID_HEADER = CommonConstants.Headers.USER_ID;
    private static final String USERNAME_HEADER = CommonConstants.Headers.USERNAME;
    private static final String USER_ROLES_HEADER = CommonConstants.Headers.USER_ROLES;
    private static final String USER_PERMISSIONS_HEADER = CommonConstants.Headers.USER_PERMISSIONS;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** Redis 操作模板 */
    private final WinterRedisTemplate winterRedisTemplate;

    /** Dubbo 远程认证服务 */
    @DubboReference
    private AuthValidationFacade authValidationFacade;

    public AuthenticationFilter(ObjectMapper objectMapper,
                                WinterRedisTemplate winterRedisTemplate) {
        this.objectMapper = objectMapper;
        this.winterRedisTemplate = winterRedisTemplate;
    }

    /**
     * Gateway 核心过滤方法
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        log.debug("处理认证请求，路径: {}", path);

        return extractToken(request)               // 1. 提取 Token
                .flatMap(this::validateTokenFormat) // 2. 校验 JWT 格式
                .flatMap(this::getUserInfo) // 3. 获取用户信息
                .flatMap(userInfo ->
                        processAuthenticatedRequest(exchange, chain, userInfo)) // 4. 构建新请求并放行
                .onErrorResume(e -> handleAuthError(response, e)); // 5. 异常统一处理
    }

    /**
     * 从请求头中提取 JWT Token
     * <p>
     * 流程说明：
     * 1. 从 HTTP 请求头中获取 Authorization 字段的值
     * 2. 检查该字段是否为空，为空则抛出"未提供认证令牌"异常
     * 3. Token 格式：
     *    - 标准格式："Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 4. 如果以 "Bearer " 开头，则截取后面的纯 Token 部分
     * 5. 将提取到的 Token 包装为 Mono 响应式对象返回
     * 
     * @param request HTTP 请求对象，包含请求头信息
     * @return Mono<String> 包装的 JWT Token 字符串
     * @throws AuthenticationException 当请求头中未提供认证令牌时抛出
     */
    private Mono<String> extractToken(ServerHttpRequest request) {
        // 从请求头中获取 Authorization 字段的值
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        // 检查 Authorization 字段是否为空
        if (StrUtil.isBlank(authHeader)) {
            return Mono.error(new AuthenticationException("未提供认证令牌"));
        }

        // "Bearer xxx" 格式，则去掉 "Bearer " 前缀，只保留 token 部分
        String token ="";
        if(authHeader.startsWith(BEARER_PREFIX)){
            token = authHeader.substring(BEARER_PREFIX.length());
        }
        return Mono.just(token);
        // 将 token 包装为 Mono 对象返回
    }

    /**
     * 校验 Token 的合法性（JWT 本地校验）
     * <p>
     * 这是一个快速的本地校验，不需要访问数据库或远程服务
     * <p>
     * 校验内容包括：
     * 1. JWT 签名是否正确（防止 Token 被篡改）
     * 2. Token 是否在有效期内（检查 exp 字段）
     * 3. Token 格式是否完整（能否解析出必要的字段）
     * 4. 是否能从 Token 中提取出用户 ID（subject 字段）
     * 
     * @param token JWT Token 字符串
     * @return Mono<String> 如果校验通过，返回原 Token；否则返回包含错误信息的 Mono.error
     * @throws AuthenticationException 当 Token 无效、过期或格式错误时抛出
     */
    private Mono<String> validateTokenFormat(String token) {

        // 第一步：校验 JWT 的签名和过期时间
        // JwtUtil.validateToken 会检查：
        // - 签名是否匹配（使用密钥验证）
        // - 是否已过期（比较当前时间和 exp 字段）
        if (!JwtUtil.validateToken(token)) {
            return Mono.error(new AuthenticationException("令牌无效或已过期"));
        }

        // 第二步：尝试从 Token 中解析出用户 ID（JWT 的 subject 字段）
        String userId = JwtUtil.getSubject(token);
        // 检查用户 ID 是否为空或只包含空白字符
        if (!StringUtils.hasText(userId)) {
            return Mono.error(new AuthenticationException("令牌格式错误"));
        }

        // 校验通过，返回原 Token
        return Mono.just(token);
    }

    /**
     * 获取用户信息（Redis 优先，远程兜底）
     * <p>
     * 采用两级获取策略，提高性能并保证可用性：
     * <p>
     * 一级：Redis 缓存
     * - 优点：速度快（毫秒级），减轻后端服务压力
     * - 适用场景：高频访问的热点数据
     * <p>
     * 二级：远程认证服务（Dubbo RPC）
     * - 当 Redis 中没有数据时触发
     * - 调用 auth-service 进行完整的 Token 校验
     * - 校验通过后，结果会被缓存到 Redis
     * <p>
     * 流程示意：
     * Token -> 提取 userId -> 构建 Redis Key -> 查询 Redis
     *                                             |
     *                                             |-- 命中 -> 返回用户信息
     *                                             |
     *                                             |-- 未命中 -> 调用远程服务 -> 返回用户信息
     * 
     * @param token JWT Token 字符串
     * @return Mono<ValidateTokenDTO> 包装的用户信息对象
     */
    private Mono<ValidateTokenDTO> getUserInfo(String token) {

        // 从 Token 中解析出用户 ID
        String userId = JwtUtil.getSubject(token);
        // 构建 Redis 缓存的 Key，格式如："winter-cloud-userInfo:12345"
        String cacheKey = buildUserCacheKey(userId);

        return getCachedUserInfo(cacheKey)          // 第一步：尝试从 Redis 缓存获取
                .switchIfEmpty(getRemoteUserInfo(token)) // 第二步：如果缓存为空，则调用远程服务
                .doOnNext(result ->
                        // 记录调试日志：成功获取到用户信息
                        log.debug("获取用户信息成功，userId: {}", result.getUserId()));
    }

    /**
     * 从 Redis 缓存中获取用户信息
     * <p>
     * 实现细节：
     * 1. 先检查 Key 是否存在（避免无效查询）
     * 2. 获取缓存的 JSON 字符串
     * 3. 将 JSON 反序列化为 ValidateTokenDTO 对象
     * 4. 如果反序列化失败（脏数据），则删除该 Key
     * <p>
     * 线程模型：
     * - Redis 操作是同步阻塞的，不能在 Gateway 的事件循环线程中执行
     * - 使用 Schedulers.boundedElastic() 将操作调度到专门的线程池
     * - boundedElastic 线程池适合 IO 密集型任务（数据库、缓存、文件等）
     * <p>
     * 返回值处理：
     * - 如果缓存命中且反序列化成功，返回 Mono<ValidateTokenDTO>
     * - 如果缓存未命中或数据异常，返回 Mono.empty()（空 Mono）
     * - 调用方可通过 switchIfEmpty() 处理空值情况
     * 
     * @param cacheKey Redis 缓存键，格式如："winter-cloud-userInfo:12345"
     * @return Mono<ValidateTokenDTO> 如果缓存命中返回用户信息，否则返回空 Mono
     */
    private Mono<ValidateTokenDTO> getCachedUserInfo(String cacheKey) {

        return Mono.fromCallable(() -> {
                    // 第二步：从 Redis 获取缓存数据（JSON 字符串）
                    Object cachedData = winterRedisTemplate.get(cacheKey);
                    if (ObjectUtil.isEmpty(cachedData)) {
                        return null; // 数据为空（理论上不应该发生，但做防御性检查）
                    }
                    // 验证存储结果
                    log.info("从 Redis 获取的 JSON: {}", cachedData);
                    try {
                        // 第三步：将 JSON 字符串反序列化为 ValidateTokenDTO 对象
                        return objectMapper.readValue(
                                cachedData.toString(), ValidateTokenDTO.class);
                    } catch (JsonProcessingException e) {
                        // 反序列化失败，说明缓存中的数据格式不正确（脏数据）
                        // 可能的原因：
                        // - 缓存的数据结构发生了变更
                        // - 人为修改了 Redis 中的数据
                        // - 序列化/反序列化版本不一致
                        log.warn("Redis 缓存数据反序列化失败，删除脏数据，key: {}", cacheKey, e);
                        winterRedisTemplate.delete(cacheKey); // 删除脏数据
                        return null; // 返回 null，让调用方走远程服务
                    }
                })
                .subscribeOn(Schedulers.boundedElastic()) // 将 Redis 操作调度到弹性线程池执行
                .filter(Objects::nonNull); // 过滤掉 null 值，将其转换为 Mono.empty()
    }

    /**
     * 调用远程认证服务校验 Token
     * <p>
     * 当 Redis 缓存未命中时，会调用此方法进行完整的 Token 校验
     * <p>
     * 调用流程：
     * 1. 通过 Dubbo RPC 调用 winter-cloud-auth 的 validateToken 接口
     * 2. winter-cloud-auth 会执行：
     *    - Token 有效性校验
     *    - 从数据库查询用户信息
     *    - 查询用户的角色和权限
     *    - 将结果缓存到 Redis（在 winter-cloud-auth 端完成(登录成功时设置)）
     * 3. 返回包含用户信息、角色、权限的完整 DTO
     * <p>
     * 线程模型：
     * - Dubbo 调用是同步阻塞的，需要放到单独的线程池执行
     * - 使用 boundedElastic() 调度器，避免阻塞 Gateway 的事件循环线程
     * <p>
     * 异常处理：
     * - 如果远程服务返回 null，表示服务异常
     * - 如果 valid 字段为 false，表示 Token 校验失败
     * - 两种情况都转换为 AuthenticationException
     * 
     * @param token JWT Token 字符串
     * @return Mono<ValidateTokenDTO> 校验成功返回用户信息，失败返回 Mono.error
     * @throws AuthenticationException 当 Token 校验失败或服务异常时抛出
     */
    private Mono<ValidateTokenDTO> getRemoteUserInfo(String token) {

        return Mono.fromCallable(() ->
                        // 通过 Dubbo 调用远程认证服务的 validateToken 方法
                        // 这是一个同步阻塞调用，会等待远程服务返回结果
                        authValidationFacade.validateToken(token))
                .subscribeOn(Schedulers.boundedElastic()) // 调度到弹性线程池执行
                .flatMap(result -> {

                    // 校验远程服务的返回结果
                    // result == null：远程服务异常，未返回数据
                    // !result.getValid()：Token 校验失败（可能是伪造、过期、已注销等）
                    if (result == null || !result.getValid()) {
                        return Mono.error(
                                new AuthenticationException(
                                        // 优先使用远程服务返回的错误信息，如果没有则使用默认消息
                                        result != null ? result.getMessage() : "Token验证失败"));
                    }

                    // 校验通过，返回用户信息
                    return Mono.just(result);
                });
    }


    /**
     * 认证成功，构建新请求并放行
     * <p>
     * 当 Token 校验通过，用户信息获取成功后，执行以下操作：
     * <p>
     * 1. 将用户信息（userId、username、roles、permissions）写入 HTTP 请求头
     * 2. 构建新的请求对象（包含用户信息的请求头）
     * 3. 用新请求替换原请求
     * 4. 将请求放行到下游服务
     * <p>
     * 为什么要将用户信息写入请求头？
     * - 下游微服务不需要再次校验 Token
     * - 下游服务可以直接从请求头获取当前用户信息
     * - 简化下游服务的认证逻辑，统一由 Gateway 处理
     * <p>
     * 请求头示例：
     * X-User-Id: 12345
     * X-Username: zhangsan
     * X-User-Roles: admin,user
     * X-User-Permissions: user:read,user:write
     * 
     * @param exchange 当前的 Web 交换对象，包含请求和响应
     * @param chain 过滤器链，用于将请求传递给下一个过滤器或目标服务
     * @param userInfo 已验证的用户信息，包含 ID、用户名、角色、权限等
     * @return Mono<Void> 表示异步处理完成
     */
    private Mono<Void> processAuthenticatedRequest(ServerWebExchange exchange,
                                                   GatewayFilterChain chain,
                                                   ValidateTokenDTO userInfo) {

        // 构建包含用户信息请求头的新请求对象
        ServerHttpRequest newRequest =
                buildRequestWithUserHeaders(exchange.getRequest(), userInfo);

        // 用新请求替换原请求，并放行到下游服务
        return chain.filter(
                exchange.mutate().request(newRequest).build());
    }

    /**
     * 将用户信息写入请求 Header
     * <p>
     * 功能说明：
     * 在原始请求的基础上，添加包含用户信息的自定义请求头
     * <p>
     * 写入的请求头包括：
     * 1. X-User-Id：用户 ID（必填）
     * 2. X-Username：用户名（必填）
     * 3. X-User-Roles：用户角色列表（可选，逗号分隔）
     * 4. X-User-Permissions：用户权限列表（可选，逗号分隔）
     * <p>
     * 数据格式示例：
     * - userId: 12345 -> X-User-Id: 12345
     * - username: zhangsan -> X-Username: zhangsan
     * - roles: ["admin", "user"] -> X-User-Roles: admin,user
     * - permissions: ["user:read", "user:write"] -> X-User-Permissions: user:read,user:write
     * <p>
     * 注意事项：
     * - 角色和权限可能为空（新用户或游客模式），需要做非空判断
     * - 集合类型的字段使用逗号分隔，下游服务需要按逗号切分解析
     * - 请求头的 Key 定义在 CommonConstants 中，保证全局统一
     * 
     * @param request 原始 HTTP 请求对象
     * @param userInfo 用户信息 DTO，包含 ID、用户名、角色、权限
     * @return ServerHttpRequest 包含用户信息请求头的新请求对象
     */
    private ServerHttpRequest buildRequestWithUserHeaders(ServerHttpRequest request,
                                                          ValidateTokenDTO userInfo) {

        // 基于原始请求创建构建器，并添加必填的用户信息请求头
        ServerHttpRequest.Builder builder = request.mutate()
                .header(USER_ID_HEADER, String.valueOf(userInfo.getUserId())) // 用户 ID
                .header(USERNAME_HEADER, userInfo.getUserName());              // 用户名

        // 添加角色信息（可选字段，需要先判断是否为空）
        if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            // 将角色列表转换为逗号分隔的字符串
            // 例如：["admin", "user"] -> "admin,user"
            builder.header(USER_ROLES_HEADER,
                    String.join(",", userInfo.getRoles()));
        }

        // 添加权限信息（可选字段，需要先判断是否为空）
        if (userInfo.getPermissions() != null && !userInfo.getPermissions().isEmpty()) {
            // 将权限列表转换为逗号分隔的字符串
            // 例如：["user:read", "user:write"] -> "user:read,user:write"
            builder.header(USER_PERMISSIONS_HEADER,
                    String.join(",", userInfo.getPermissions()));
        }

        // 构建并返回新的请求对象
        return builder.build();
    }

    /**
     * 统一处理认证异常
     * <p>
     * 在整个认证流程中，如果任何一步出现异常，都会被路由到这个方法
     * <p>
     * 异常类型分类：
     * 1. AuthenticationException：业务层面的认证失败
     *    - 未提供 Token
     *    - Token 格式错误
     *    - Token 已过期
     *    - Token 校验失败
     *    - 用户不存在或已禁用
     * <p>
     * 2. 其他异常：系统层面的异常
     *    - Redis 连接失败
     *    - Dubbo 调用超时
     *    - 网络异常
     *    - 反序列化失败
     * <p>
     * 处理策略：
     * - 认证异常：返回具体的错误信息（如"令牌已过期"）
     * - 系统异常：返回通用的错误信息（"认证服务异常"），避免暴露内部实现细节
     * 
     * @param response HTTP 响应对象，用于返回错误信息给客户端
     * @param error 捕获到的异常对象
     * @return Mono<Void> 包含错误响应的 Mono
     */
    private Mono<Void> handleAuthError(ServerHttpResponse response,
                                       Throwable error) {

        // 区分异常类型，决定返回的错误信息
        String message = error instanceof AuthenticationException
                ? error.getMessage()           // 认证异常：返回具体错误信息
                : "认证服务异常";                // 系统异常：返回通用错误信息

        // 记录异常日志（便于排查问题）
        if (error instanceof AuthenticationException) {
            log.warn("认证失败: {}", message);
        } else {
            log.error("认证过程中发生系统异常", error);
        }

        // 构建并返回 401 未授权响应
        return unauthorizedResponse(response, message);
    }


    /**
     * 返回 401 未授权响应
     * <p>
     * 当认证失败时，向客户端返回统一格式的 JSON 错误响应
     * <p>
     * HTTP 状态码：401 Unauthorized
     * - 表示请求需要用户认证，或认证失败
     * - 客户端需要重新登录或提供有效的 Token
     * <p>
     * 响应格式（JSON）：
     * {
     *   "code": 401,                    // 业务状态码
     *   "message": "令牌已过期",         // 错误描述
     *   "data": null                    // 数据字段（认证失败时为 null）
     * }
     * <p>
     * 处理流程：
     * 1. 设置 HTTP 状态码为 401
     * 2. 设置响应头 Content-Type 为 JSON 格式
     * 3. 构建统一的错误响应体
     * 4. 将响应体序列化为 JSON 字节数组
     * 5. 包装为 DataBuffer 并写入响应流
     * 6. 如果序列化失败，返回空响应体
     * 
     * @param response HTTP 响应对象
     * @param message 错误消息，描述认证失败的具体原因
     * @return Mono<Void> 表示响应写入完成
     */
    private Mono<Void> unauthorizedResponse(ServerHttpResponse response,
                                            String message) {

        // 第一步：设置 HTTP 状态码为 401（Unauthorized）
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        // 第二步：设置响应头，指定内容类型为 JSON，字符编码为 UTF-8
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        // 第三步：构建统一格式的响应体
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);        // 业务状态码（与 HTTP 状态码保持一致）
        body.put("message", message); // 错误描述信息（如"令牌已过期"）
        body.put("data", null);       // 数据字段（认证失败时无数据，设为 null）

        try {
            // 第四步：将 Map 序列化为 JSON 字节数组
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            
            // 第五步：将字节数组包装为 DataBuffer（Spring WebFlux 的响应数据格式）
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            
            // 第六步：将 DataBuffer 写入响应流，并返回 Mono 表示异步完成
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // 异常处理：如果 JSON 序列化失败，记录日志并返回空响应
            log.error("构建认证失败响应时发生异常", e);
            return response.setComplete(); // 直接完成响应，不写入响应体
        }
    }

    /**
     * 过滤器优先级（值越小越靠前）
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 自定义认证异常
     */
    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}