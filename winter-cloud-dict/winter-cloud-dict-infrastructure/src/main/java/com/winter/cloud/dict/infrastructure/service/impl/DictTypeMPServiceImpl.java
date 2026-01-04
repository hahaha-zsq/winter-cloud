package com.winter.cloud.dict.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.dict.infrastructure.entity.DictTypePO;
import com.winter.cloud.dict.infrastructure.mapper.DictTypeMapper;
import com.winter.cloud.dict.infrastructure.service.IDictTypeMPService;
import org.springframework.stereotype.Service;

@Service
public class DictTypeMPServiceImpl extends ServiceImpl<DictTypeMapper, DictTypePO> implements IDictTypeMPService {
}
