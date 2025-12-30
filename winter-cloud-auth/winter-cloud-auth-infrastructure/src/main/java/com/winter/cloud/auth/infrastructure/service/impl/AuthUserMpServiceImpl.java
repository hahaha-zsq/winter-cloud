package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthUserMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthUserMpService;
import org.springframework.stereotype.Service;

@Service
public class AuthUserMpServiceImpl extends ServiceImpl<AuthUserMapper, AuthUserPO> implements IAuthUserMpService {
}