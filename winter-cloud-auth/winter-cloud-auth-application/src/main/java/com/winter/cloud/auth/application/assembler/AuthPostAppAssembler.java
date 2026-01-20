package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPostAppAssembler {


    List<PostResponseDTO> toDTOList(List<AuthPostDO> authPostPOList);
}
