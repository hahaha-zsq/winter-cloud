package com.winter.cloud.dict.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.infrastructure.assembler.DictDataInfraAssembler;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import com.winter.cloud.dict.infrastructure.service.IDictDataMPService;
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
    private final IDictDataMPService dictDataMPService;
    private final DictDataInfraAssembler dictDataInfraAssembler;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<DictDataDO> getDictDataByType(Long dictType, String status) {
        return dictDataMapper.getDictDataByType(dictType, status);
    }

    @Override
    public List<DictDataDO> dictValueDynamicQueryList(DictQuery dictQuery) {
        LambdaQueryWrapper<DictDataPO> queryWrapper = new LambdaQueryWrapper<DictDataPO>()
                .eq(ObjectUtil.isNotEmpty(dictQuery.getDictTypeId()), DictDataPO::getDictTypeId, dictQuery.getDictTypeId())
                .eq(ObjectUtil.isNotEmpty(dictQuery.getStatus()), DictDataPO::getStatus, dictQuery.getStatus())
                .like(ObjectUtil.isNotEmpty(dictQuery.getDictLabel()), DictDataPO::getDictLabel, dictQuery.getDictLabel())
                .like(ObjectUtil.isNotEmpty(dictQuery.getDictValue()), DictDataPO::getDictValue, dictQuery.getDictValue());
        List<DictDataPO> list = dictDataMPService.list(queryWrapper);
        return dictDataInfraAssembler.toDOList(list);
    }
}
