package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证控制器
 * DDD Interfaces Layer - HTTP 协议适配层
 * 供网关通过 HTTP 协议调用
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/auth")
public class AuthUserController implements AuthValidationFacade {


    @PostMapping("/validate")
    @Override
    public ValidateTokenDTO validateToken(String token) {
        log.info("=== Dubbo RPC 接口调用开始 ===");
        log.info("接收到 Token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");

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

    @GetMapping("/test")
    public String test() {
        return "hello world";
    }
}
