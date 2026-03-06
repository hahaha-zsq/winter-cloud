package com.winter.cloud.dict.domain.repository;


import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.command.UpsertDictTypeCommand;
import com.winter.cloud.dict.api.dto.query.DictTypeQuery;
import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.domain.model.entity.DictTypeDO;

import java.util.List;

public interface DictTypeRepository {

    PageDTO<DictTypeDO> dictTypePage(DictTypeQuery dictTypeQuery);

    Boolean hasDuplicateDictType(DictTypeDO aDo);

    Boolean dictTypeSave(UpsertDictTypeCommand upsertDictTypeCommand);

    Boolean dictTypeUpdate(UpsertDictTypeCommand upsertDictTypeCommand);

    Boolean dictTypeDelete(List<Long> ids);

    List<DictTypeDO> dictTypeList(DictTypeQuery dictTypeQuery);
}