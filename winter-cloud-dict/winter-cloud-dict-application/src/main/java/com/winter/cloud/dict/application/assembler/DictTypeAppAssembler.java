package com.winter.cloud.dict.application.assembler;

import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.domain.model.entity.DictTypeDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictTypeAppAssembler {

    List<DictTypeDTO> toDictTypeDTOList(List<DictTypeDO> dictTypeDOList);

    DictTypeDTO toDictTypeDTO(DictTypeDO dictTypeDO);
}
