package com.winter.cloud.dict.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


    @PostConstruct
    public void init() {
        log.info("预加载字典数据存入redis");
        List<DictDataPO> list = dictDataMapper.selectList(null);
        Map<Long, List<DictDataPO>> collect = list.stream().collect(Collectors.groupingBy(DictDataPO::getDictTypeId));
        collect.forEach((k, v) -> {
            String data = null;
            try {
                data = objectMapper.writeValueAsString(v);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            winterRedisTemplate.set(CommonConstants.Redis.DICT_KEY+CommonConstants.Redis.SPLIT+k,data);
        });

    }
}
