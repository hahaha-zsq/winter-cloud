package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.infrastructure.service.AuthValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * DDD Interfaces Layer - HTTP 协议适配层
 * 供网关通过 HTTP 协议调用
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthUserController {

    private final AuthValidationService authValidationService;

    /**
     * HTTP 接口：验证 Token
     */
    @PostMapping("/validate")
    public ValidateTokenDTO validateToken(@RequestBody String token) {
        log.info("HTTP 接口调用：验证 Token");
        return authValidationService.validateToken(token);
    }

    @GetMapping("/test")
    public String test() {
        return "hello world";
    }
}
