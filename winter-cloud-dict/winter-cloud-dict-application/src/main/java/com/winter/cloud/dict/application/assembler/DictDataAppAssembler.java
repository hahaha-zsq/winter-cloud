package com.winter.cloud.dict.application.assembler;

import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictDataAppAssembler {

    List<DictDataDTO> toDictDataDTOList(List<DictDataDO> dictDataDOList);
}
