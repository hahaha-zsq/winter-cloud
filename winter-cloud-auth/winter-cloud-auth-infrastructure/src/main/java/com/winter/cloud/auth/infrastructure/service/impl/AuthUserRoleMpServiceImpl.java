package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthUserRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthUserRoleMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthUserRoleMpService;
import org.springframework.stereotype.Service;

@Service
public class AuthUserRoleMpServiceImpl extends ServiceImpl<AuthUserRoleMapper, AuthUserRolePO> implements IAuthUserRoleMpService {
}