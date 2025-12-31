package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthMenuPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthMenuMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthMenuMpService;
import org.springframework.stereotype.Service;

@Service
public class AuthMenuMpServiceImpl extends ServiceImpl<AuthMenuMapper, AuthMenuPO> implements IAuthMenuMpService {
}