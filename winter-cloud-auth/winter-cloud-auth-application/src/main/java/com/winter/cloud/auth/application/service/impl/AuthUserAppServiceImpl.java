package com.winter.cloud.auth.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.command.UserLoginCommand;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.response.LoginResponseDTO;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.application.assembler.AuthUserAppAssembler;
import com.winter.cloud.auth.application.service.AuthUserAppService;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.domain.repository.AuthUserRepository;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.util.JwtUtil;
import com.zsq.winter.encrypt.util.CryptoUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate; // 使用你的 Redis 工具
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.winter.cloud.common.enums.ResultCodeEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserAppServiceImpl implements AuthUserAppService {

    private final AuthUserRepository authUserRepository;
    private final AuthUserAppAssembler authUserAppAssembler;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean register(UserRegisterCommand command) {
        // 1. 校验是否存在重复的用户信息
        if (authUserRepository.hasDuplicateUser(command)) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), "用户名已存在！");
        }
        // 2. 创建领域对象
        AuthUserDO authUserDO = authUserAppAssembler.toDO(command);
        // 3. 密码加密
        String encryptedPwd = CryptoUtil.winterMd5Hex16(authUserDO.getPassword());
        authUserDO.setPassword(encryptedPwd);

        authUserDO.setStatus("1"); // 正常
        authUserDO.setDelFlag("0"); // 未删除

        // 4. 保存
        return authUserRepository.save(authUserDO);
    }

    @Override
    public LoginResponseDTO login(UserLoginCommand command) throws JsonProcessingException {
        AuthUserDO authUserDO = authUserRepository.findByUserName(command.getUserName());
        // 查询用户名是否存在，不存在抛出异常，存在进行密码校验
        if (ObjectUtils.isEmpty(authUserDO)) {
            throw new BusinessException(NOT_FOUND);
        }
        // 密码校验
        if (!authUserDO.verifyPassword(command.getPassword(), authUserDO.getPassword())) {
            throw new BusinessException(LOGIN_FAILED);
        }
        // 账号状态校验
        if (!authUserDO.getStatus().equals("1")) {
            throw new BusinessException(DISABLED.getCode(), "用户已停用");
        }
        // 生成token
        HashMap<String, Object> claim = new HashMap<>();
        claim.put(CommonConstants.Claim.NAME, authUserDO.getUserName());
        String token = JwtUtil.generateToken(String.valueOf(authUserDO.getId()), claim, CommonConstants.Redis.EXPIRATION_TIME);
        // todo 数据库获取用户的角色和权限信息
        ArrayList<String> roleList = new ArrayList<>();
        ArrayList<String> permissionsList = new ArrayList<>();
        ValidateTokenDTO validateTokenDTO = ValidateTokenDTO.builder()
                .valid(true)
                .userId(authUserDO.getId())
                .userName(authUserDO.getUserName())
                .roles(roleList)
                .permissions(permissionsList).build();

        // 序列化
        String value = objectMapper.writeValueAsString(validateTokenDTO);
        // 缓存用户信息可以设置过期时间也可以不设置，网关先校验token有没有过期，没过期才会去用用户id去缓存查找
        winterRedisTemplate.set(CommonConstants.buildUserCacheKey(authUserDO.getId().toString()), value, CommonConstants.Redis.EXPIRATION_TIME, TimeUnit.MICROSECONDS);
        return LoginResponseDTO.builder()
                .token(token)
                .nickName(authUserDO.getNickName())
                .userId(authUserDO.getId())
                .userName(authUserDO.getUserName())
                .build();
    }
}