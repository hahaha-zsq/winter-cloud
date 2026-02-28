package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthUserDeptPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthUserDeptMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthUserDeptMpService;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDeptMpServiceImpl extends ServiceImpl<AuthUserDeptMapper, AuthUserDeptPO> implements IAuthUserDeptMpService {
}