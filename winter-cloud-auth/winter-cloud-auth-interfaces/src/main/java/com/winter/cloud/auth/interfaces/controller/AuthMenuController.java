package com.winter.cloud.auth.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AuthMenuController {

}
