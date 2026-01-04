package com.winter.cloud.dict.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import com.winter.cloud.dict.infrastructure.service.IDictDataMPService;
import org.springframework.stereotype.Service;

@Service
public class DictDataMPServiceImpl extends ServiceImpl<DictDataMapper, DictDataPO> implements IDictDataMPService {
}
