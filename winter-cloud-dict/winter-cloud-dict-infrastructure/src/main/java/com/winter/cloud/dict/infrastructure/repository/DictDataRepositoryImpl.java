package com.winter.cloud.dict.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DictDataRepositoryImpl implements DictDataRepository {
    private final DictDataMapper dictDataMapper;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    @Override
    public List<DictDataDO> getDictDataByType(Long dictType, String status) {
        return dictDataMapper.getDictDataByType(dictType,status);
    }
}
