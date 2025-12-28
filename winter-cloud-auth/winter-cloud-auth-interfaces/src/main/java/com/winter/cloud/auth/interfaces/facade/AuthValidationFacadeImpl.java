package com.winter.cloud.auth.interfaces.facade;

import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.auth.infrastructure.service.AuthValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 认证验证 Dubbo 服务实现
 * DDD Interfaces Layer - Dubbo RPC 协议适配层
 * 供其他微服务通过 Dubbo 协议调用
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class AuthValidationFacadeImpl implements AuthValidationFacade {

    private final AuthValidationService authValidationService;

    @Override
    public ValidateTokenDTO validateToken(String token) {
        log.info("=== Dubbo RPC 接口调用开始 ===");
        log.info("接收到 Token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
        
        try {
            ValidateTokenDTO result = authValidationService.validateToken(token);
            log.info("=== Dubbo RPC 调用成功，返回结果: valid={}, userId={} ===", 
                result.getValid(), result.getUserId());
            return result;
        } catch (Exception e) {
            log.error("=== Dubbo RPC 调用异常 ===", e);
            throw e;
        }
    }
}
