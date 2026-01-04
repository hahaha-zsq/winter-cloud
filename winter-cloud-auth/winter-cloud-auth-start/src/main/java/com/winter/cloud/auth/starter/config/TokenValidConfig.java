package com.winter.cloud.auth.starter.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.common.util.JwtUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.security.config.TokenAuthenticator;
import com.zsq.winter.security.model.ValidateToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

import static com.winter.cloud.common.constants.CommonConstants.buildUserCacheKey;

/**
 * Token 验证配置类(自定哟winter-security-spring-boot-starter里面的token校验器)
 * <p>
 * 实现 TokenAuthenticator 接口，用于对客户端传入的 Token 进行验证。
 * 支持从 Redis 缓存读取用户信息，如果缓存不存在或失效，则调用认证服务进行验证。
 * 验证成功后返回用户信息和权限列表。
 * </p>
 */
@Slf4j
@Service
public class TokenValidConfig implements TokenAuthenticator {

    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthValidationFacade authValidationFacade;

    /**
     * 构造方法注入依赖
     *
     * @param winterRedisTemplate  Redis 操作模板
     * @param objectMapper         JSON 序列化工具
     * @param authValidationFacade 认证服务接口
     */
    public TokenValidConfig(WinterRedisTemplate winterRedisTemplate,
                            ObjectMapper objectMapper,
                            AuthValidationFacade authValidationFacade) {
        this.winterRedisTemplate = winterRedisTemplate;
        this.objectMapper = objectMapper;
        this.authValidationFacade = authValidationFacade;
    }

    /**
     * 认证方法
     *
     * @param token 客户端传入的 Token
     * @return AuthResult 验证结果，包含用户信息和权限
     */
    @Override
    public AuthResult authenticate(String token) {
        // 1. 验证 Token 是否有效（是否过期或格式错误）
        if (!JwtUtil.validateToken(token)) {
            return AuthResult.failure("令牌无效或已过期");
        }

        // 2. 从 Token 中解析出 userId
        String userId = JwtUtil.getSubject(token);
        if (ObjectUtils.isEmpty(userId)) {
            return AuthResult.failure("解析 token 失败");
        }

        // 3. 构建 Redis 缓存 Key
        String cacheKey = buildUserCacheKey(userId);

        // 4. 尝试从 Redis 获取缓存数据
        Object cachedData = winterRedisTemplate.get(cacheKey);

        // 5. 使用 Optional + Lambda 处理 Redis 缓存解析和远程调用
        ValidateToken validateToken = Optional.ofNullable(cachedData)
                // map(Object::toString) 如果 cachedData 不为 null，将其转为 String（因为 Redis 存的是 JSON 字符串）
                .map(Object::toString)
                .flatMap(json -> {
                    try {
                        ValidateTokenDTO dto = objectMapper.readValue(json, ValidateTokenDTO.class);
                        return Optional.of(dto);
                    } catch (JsonProcessingException e) {
                        // 缓存脏数据，删除并返回空
                        log.warn("Redis 缓存数据反序列化失败，删除脏数据，key: {}", cacheKey, e);
                        winterRedisTemplate.delete(cacheKey);
                        return Optional.empty();
                    }
                })
                // 如果缓存为空或失效，调用远程认证服务
                .filter(ValidateTokenDTO::getValid)
                .or(() -> Optional.ofNullable(authValidationFacade.validateToken(token))
                        .filter(ValidateTokenDTO::getValid))
                // 将 ValidateTokenDTO 转换为内部模型 ValidateToken
                .map(this::mapToValidateToken)
                .orElse(null);

        // 6. 返回认证结果
        if (!ObjectUtils.isEmpty(validateToken)) {
            return AuthResult.success(validateToken);
        } else {
            return AuthResult.failure("解析数据错误");
        }
    }

    /**
     * 将 ValidateTokenDTO 转换为内部模型 ValidateToken
     *
     * @param dto 远程或缓存获取的 ValidateTokenDTO
     * @return ValidateToken 内部使用的用户信息和权限对象
     */
    private ValidateToken mapToValidateToken(ValidateTokenDTO dto) {
        return ValidateToken.builder()
                .valid(true)
                .userId(dto.getUserId())
                .userName(dto.getUserName())
                .roles(dto.getRoles())
                .permissions(dto.getPermissions())
                .build();
    }
}