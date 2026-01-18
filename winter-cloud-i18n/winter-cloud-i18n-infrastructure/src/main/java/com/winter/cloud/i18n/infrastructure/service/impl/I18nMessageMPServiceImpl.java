package com.winter.cloud.i18n.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import com.winter.cloud.i18n.infrastructure.mapper.I18nMessageMapper;
import com.winter.cloud.i18n.infrastructure.service.II18nMessageMPService;
import org.springframework.stereotype.Service;

@Service
public class I18nMessageMPServiceImpl  extends ServiceImpl<I18nMessageMapper, I18nMessagePO> implements II18nMessageMPService {

}
