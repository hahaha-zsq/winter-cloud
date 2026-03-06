package com.winter.cloud.dict.infrastructure.assembler;

import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.domain.model.entity.DictTypeDO;
import com.winter.cloud.dict.infrastructure.entity.DictTypePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictTypeInfraAssembler {

    DictTypeDO toDO(DictTypePO dictTypePO);

    List<DictTypeDO> toDOList(List<DictTypePO> dictTypePOList);

    DictTypePO toPO(DictTypeDO dictTypeDO);

    List<DictTypePO> toPOList(List<DictTypeDO> dictTypeDOList);

}
