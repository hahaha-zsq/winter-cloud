package com.winter.cloud.dict.application.service.impl;

import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.application.assembler.DictDataAppAssembler;
import com.winter.cloud.dict.application.service.DictAppService;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictAppServiceImpl implements DictAppService {
    private final DictDataRepository dictDataRepository;
    private final DictDataAppAssembler dictDataAppAssembler;

    @Override
    public List<DictDataDTO> getDictDataByType(Long dictType, String status) {
        List<DictDataDO> data = dictDataRepository.getDictDataByType(dictType, status);
        return dictDataAppAssembler.toDictDataDTOList(data);
    }

    @Override
    public List<DictDataDTO> dictValueDynamicQueryList(DictQuery dictQuery) {
        List<DictDataDO> data = dictDataRepository.dictValueDynamicQueryList(dictQuery);
        return dictDataAppAssembler.toDictDataDTOList(data);
    }
}
