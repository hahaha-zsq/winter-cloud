package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthRoleMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMPService;
import org.springframework.stereotype.Service;

@Service
public class AuthRoleMpServiceImpl extends ServiceImpl<AuthRoleMapper, AuthRolePO> implements IAuthRoleMPService {
}