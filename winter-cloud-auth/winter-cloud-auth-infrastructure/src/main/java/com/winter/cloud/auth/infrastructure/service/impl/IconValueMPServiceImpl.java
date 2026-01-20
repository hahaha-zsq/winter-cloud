package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.IconValuePO;

import com.winter.cloud.auth.infrastructure.mapper.IconValueMapper;
import com.winter.cloud.auth.infrastructure.service.IconValueMPService;
import org.springframework.stereotype.Service;

@Service
public class IconValueMPServiceImpl extends ServiceImpl<IconValueMapper, IconValuePO> implements IconValueMPService {

}