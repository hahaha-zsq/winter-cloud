package com.winter.cloud.dict.application.service;


import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.command.UpsertDictDataCommand;
import com.winter.cloud.dict.api.dto.command.UpsertDictTypeCommand;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.api.dto.query.DictTypeQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.dto.response.DictTypeDTO;

import java.util.List;

public interface DictAppService {


    List<DictDataDTO> getDictDataByType(Long dictType, String status);

    List<DictDataDTO> dictValueDynamicQueryList(DictDataQuery dictQuery);

    PageDTO<DictTypeDTO> dictTypePage(DictTypeQuery dictTypeQuery);

    Boolean dictTypeSave(UpsertDictTypeCommand upsertDictTypeCommand);

    Boolean dictTypeUpdate(UpsertDictTypeCommand upsertDictTypeCommand);

    Boolean dictTypeDelete(List<Long> ids);

    PageDTO<DictDataDTO> dictDataPage(DictDataQuery dictQuery);

    Boolean dictDataSave(List<UpsertDictDataCommand> upsertDictDataCommand);

    Boolean dictDataUpdate(UpsertDictDataCommand upsertDictDataCommand);

    Boolean dictDataDelete(List<Long> ids);

    List<DictTypeDTO> dictTypeList(DictTypeQuery dictTypeQuery);
}