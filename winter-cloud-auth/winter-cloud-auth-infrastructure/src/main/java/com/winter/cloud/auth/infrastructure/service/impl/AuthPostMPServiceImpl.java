package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthPostPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthPostMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthPostMPService;
import org.springframework.stereotype.Service;

@Service
public class AuthPostMPServiceImpl extends ServiceImpl<AuthPostMapper, AuthPostPO> implements IAuthPostMPService {

}
