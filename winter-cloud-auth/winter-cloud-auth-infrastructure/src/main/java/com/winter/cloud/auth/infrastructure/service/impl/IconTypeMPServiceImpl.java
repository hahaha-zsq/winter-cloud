package com.winter.cloud.auth.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.auth.infrastructure.entity.IconTypePO;
import com.winter.cloud.auth.infrastructure.mapper.IconTypeMapper;
import com.winter.cloud.auth.infrastructure.service.IconTypeMPService;
import org.springframework.stereotype.Service;

@Service
public class IconTypeMPServiceImpl extends ServiceImpl<IconTypeMapper, IconTypePO> implements IconTypeMPService {

}