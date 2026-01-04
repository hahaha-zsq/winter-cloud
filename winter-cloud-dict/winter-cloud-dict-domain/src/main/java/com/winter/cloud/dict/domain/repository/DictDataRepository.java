package com.winter.cloud.dict.domain.repository;


import com.winter.cloud.dict.domain.model.entity.DictDataDO;

import java.util.List;

public interface DictDataRepository {

    List<DictDataDO> getDictDataByType(Long dictType, String status);
}