package com.winter.cloud.auth.infrastructure.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthDeptMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthDeptMPService;
import org.springframework.stereotype.Service;


@Service
public class AuthDeptMPServiceImpl extends ServiceImpl<AuthDeptMapper, AuthDeptPO> implements IAuthDeptMPService {

}
