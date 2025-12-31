package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthRoleMenuPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthRoleMenuMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMenuMpService;
import org.springframework.stereotype.Service;

@Service
public class AuthRoleMenuMpServiceImpl extends ServiceImpl<AuthRoleMenuMapper, AuthRoleMenuPO> implements IAuthRoleMenuMpService {
}