package com.winter.cloud.auth.application.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.winter.cloud.auth.api.dto.command.UserLoginCommand;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.response.LoginResponseDTO;

/**
 * 认证应用服务接口
 * 职责：定义认证模块的核心用例（用户注册、登录等）
 * * @author winter
 */
public interface AuthUserAppService {

    /**
     * 用户注册
     *
     * @param command 注册命令参数（包含用户名、密码、昵称等）
     */
    Boolean register(UserRegisterCommand command);

    /**
     * 用户登录
     *
     * @param command 登录命令参数（包含用户名、密码）
     * @return 登录成功后的响应对象（包含Token、用户信息等）
     */
    LoginResponseDTO login(UserLoginCommand command) throws JsonProcessingException;
}