package com.winter.cloud.auth.infrastructure.service;

import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证验证应用服务
 * DDD Application Layer - 负责业务逻辑编排
 */
@Slf4j
@Service
public class AuthValidationService {

    /**
     * 验证 Token 并返回用户信息
     * 
     * @param token JWT Token
     * @return 验证结果
     */
    public ValidateTokenDTO validateToken(String token) {
        log.info("应用服务：验证 Token");

        ValidateTokenDTO result = new ValidateTokenDTO();

        try {
            // 1. 验证 Token 是否为空
            if (!StringUtils.hasText(token)) {
                result.setValid(false);
                result.setMessage("Token 为空");
                return result;
            }

            // 2. 验证 Token 有效性
            if (!JwtUtil.validateToken(token)) {
                result.setValid(false);
                result.setMessage("Token 无效或已过期");
                return result;
            }

            // 3. 解析 Token 获取用户信息
            String subject = JwtUtil.getSubject(token);
            String userName = (String) JwtUtil.getClaim(token, "userName");

            if (!StringUtils.hasText(subject)) {
                result.setValid(false);
                result.setMessage("Token 解析失败");
                return result;
            }

            Long userId = Long.parseLong(subject);

            // 4. 验证用户状态（TODO: 从领域服务或仓储获取）
            // AuthUserDO userDO = authUserRepository.findById(userId);
            // if (userDO == null || userDO.getStatus() == 1) {
            //     result.setValid(false);
            //     result.setMessage("用户不存在或已被禁用");
            //     return result;
            // }

            // 5. 获取用户角色和权限（TODO: 从领域服务获取）
            // List<String> roleKeyList = authRoleService.getRoleKeyListByUserId(userId);
            // List<String> permissionKeyList = authPermissionService.getPermissionsByUserId(userId);
            List<String> roleKeyList = new ArrayList<>();
            List<String> permissionKeyList = new ArrayList<>();


            // 7. 构建返回结果
            result.setValid(true);
            result.setUserId(userId);
            result.setUserName(userName);
            result.setRoles(roleKeyList);
            result.setPermissions(permissionKeyList);
            result.setMessage("Token 有效");

            log.info("Token 验证成功，用户ID: {}, 用户名: {}", userId, userName);
            return result;

        } catch (Exception e) {
            log.error("验证 Token 失败", e);
            result.setValid(false);
            result.setMessage("Token 验证异常: " + e.getMessage());
            return result;
        }
    }
}
