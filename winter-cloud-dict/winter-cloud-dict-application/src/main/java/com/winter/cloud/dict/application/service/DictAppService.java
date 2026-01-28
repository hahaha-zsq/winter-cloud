package com.winter.cloud.dict.application.service;


import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;

import java.util.List;

public interface DictAppService {


    List<DictDataDTO> getDictDataByType(Long dictType, String status);

    List<DictDataDTO> dictValueDynamicQueryList(DictQuery dictQuery);
}