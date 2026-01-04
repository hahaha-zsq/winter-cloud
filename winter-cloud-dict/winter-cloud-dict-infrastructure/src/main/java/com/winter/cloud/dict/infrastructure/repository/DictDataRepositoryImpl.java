package com.winter.cloud.dict.infrastructure.repository;

import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class DictDataRepositoryImpl implements DictDataRepository {
    private final DictDataMapper dictDataMapper;

    @Override
    public List<DictDataDO> getDictDataByType(Long dictType, String status) {
        return dictDataMapper.getDictDataByType(dictType,status);
    }
}
