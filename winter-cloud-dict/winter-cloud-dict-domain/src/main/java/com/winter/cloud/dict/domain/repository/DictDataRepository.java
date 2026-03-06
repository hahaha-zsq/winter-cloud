package com.winter.cloud.dict.domain.repository;


import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;

import java.util.List;

public interface DictDataRepository {

    List<DictDataDO> getDictDataByType(Long dictType, String status);

    List<DictDataDO> dictValueDynamicQueryList(DictDataQuery dictQuery);

    Boolean hasDuplicateDictData(DictDataDO aDo);


    PageDTO<DictDataDO> dictDataPage(DictDataQuery dictQuery);

    Boolean dictDataSave(List<DictDataDO> dictDataDOList);

    Boolean dictDataUpdate(DictDataDO dictDataDO);

    Boolean dictDataDelete(List<Long> ids);
}