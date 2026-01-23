package com.winter.cloud.auth.interfaces.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.winter.cloud.auth.api.dto.command.UserLoginCommand;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.LoginResponseDTO;
import com.winter.cloud.auth.api.dto.response.UserResponseDTO;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.auth.application.service.AuthUserAppService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.common.util.JwtUtil;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private final AuthUserAppService authUserAppService;
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    /**
     * 验证 Token 有效性并返回用户权限信息
     * <p>
     * 该接口用于网关层调用，验证用户请求中携带的 JWT Token 是否有效，
     * 并返回用户的身份信息、角色和权限列表，供网关进行鉴权判断。
     * </p>
     * 
     * <p>验证流程：</p>
     * <ol>
     *   <li>校验 Token 是否为空</li>
     *   <li>校验 Token 格式是否正确及是否过期</li>
     *   <li>解析 Token 获取用户 ID 和用户名</li>
     *   <li>获取用户的角色和权限信息</li>
     *   <li>构建并返回验证结果</li>
     * </ol>
     *
     * @param token JWT Token 字符串
     * @return ValidateTokenDTO Token 验证结果，包含有效性、用户信息、角色和权限
     */
    @Override
    public ValidateTokenDTO validateToken(String token) {
        log.info("=== Token 验证接口调用 ===");
        log.info("接收到 Token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");

        try {
            // 步骤1: 校验 Token 是否为空
            if (!StringUtils.hasText(token)) {
                log.warn("Token 验证失败: Token 为空");
                return buildFailureResult("Token 为空");
            }

            // 步骤2: 校验 Token 格式和有效期
            if (!JwtUtil.validateToken(token)) {
                log.warn("Token 验证失败: Token 无效或已过期");
                return buildFailureResult("Token 无效或已过期");
            }

            // 步骤3: 解析 Token 获取用户基本信息
            String subject = JwtUtil.getSubject(token);

            // 校验 Token 解析结果
            if (!StringUtils.hasText(subject)) {
                log.warn("Token 验证失败: 无法解析出用户 ID");
                return buildFailureResult("Token 解析失败");
            }

            String userName = (String) JwtUtil.getClaim(token, CommonConstants.Claim.NAME);

            if (!StringUtils.hasText(userName)) {
                log.warn("Token 验证失败: 获取用户名失败");
                return buildFailureResult("Token 解析失败");
            }

            // 转换用户 ID
            Long userId;
            try {
                userId = Long.parseLong(subject);
            } catch (NumberFormatException e) {
                log.error("Token 验证失败: 用户 ID 格式错误, subject: {}", subject);
                return buildFailureResult("Token 中用户 ID 格式错误");
            }

            // 步骤4: 获取用户角色和权限信息
            // 委托给应用服务层处理，获取完整的用户权限信息
            ValidateTokenDTO result = authUserAppService.generateUserInfo(userId, userName);

            // 设置验证成功消息
            result.setMessage("Token 有效");

            log.info("Token 验证成功 - 用户ID: {}, 用户名: {}, 角色数: {}, 权限数: {}", 
                    userId, userName, 
                    result.getRoles() != null ? result.getRoles().size() : 0,
                    result.getPermissions() != null ? result.getPermissions().size() : 0);
            return result;

        } catch (NumberFormatException e) {
            // 数字转换异常（用户 ID 解析失败）
            log.error("Token 验证失败: 用户 ID 解析异常", e);
            return buildFailureResult("Token 中用户 ID 格式错误");
        } catch (Exception e) {
            // 捕获所有未预期的异常，确保接口稳定性
            log.error("Token 验证过程发生异常", e);
            return buildFailureResult("Token 验证异常: " + e.getMessage());
        }
    }

    /**
     * 构建 Token 验证失败的返回结果
     *
     * @param message 失败原因描述
     * @return ValidateTokenDTO 失败结果对象
     */
    private ValidateTokenDTO buildFailureResult(String message) {
        return ValidateTokenDTO.builder()
                .valid(false)
                .message(message)
                .build();
    }


    /**
     * 注册
     *
     * @param command 注册命令
     * @return 注册结果
     */
    @PostMapping("/register")
    public Response<Boolean> register(@RequestBody @Validated UserRegisterCommand command) {
        Boolean register = authUserAppService.register(command);
        if (!register) {
            return Response.fail(ResultCodeEnum.FAIL_LANG.getCode(),i18nMessageFacade.getMessage(ResultCodeEnum.FAIL_LANG.getMessage()));
        }
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage()),null);
    }

    /**
     * 登录
     *
     * @param command 登录命令
     * @return 登录结果
     */
    @PostMapping("/login")
    public Response<LoginResponseDTO> login(@RequestBody @Validated UserLoginCommand command) throws JsonProcessingException {
        LoginResponseDTO loginDTO = authUserAppService.login(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage()),loginDTO);
    }


    /**
     * 分页查询用户列表
     * <p>
     * 查询逻辑说明：
     * 1. 模糊查询：用户名称、用户昵称
     * 2. 精确查询：手机号、邮箱、性别、状态、职位
     * 3. 严格匹配：部门、角色（如果选择了多个，用户必须同时拥有这些部门/角色）
     * 4. 结果增强：返回的部门信息包含父子层级结构
     * </p>
     */
    @PostMapping("/userPage")
    public Response<PageDTO<UserResponseDTO>> userPage(@RequestBody UserQuery userQuery) {
        PageDTO<UserResponseDTO> data = authUserAppService.userPage(userQuery);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage()),
                data
        );
    }
}
