package com.winter.cloud.auth.api.facade;

import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;

/**
 * 认证验证服务 - Dubbo RPC 接口
 * 供网关等其他服务调用，用于验证 token 和获取用户信息
 */
public interface AuthValidationFacade {
    
    /**
     * 验证 token 是否有效
     * @param token JWT token
     */
    ValidateTokenDTO validateToken(String token);
}
