package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.command.UpsertPostCommand;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPostAppAssembler {
    AuthPostDO toDO(UpsertPostCommand command);


    List<PostResponseDTO> toDTOList(List<AuthPostDO> authPostPOList);
    List<AuthPostDO> toDOList(List<PostResponseDTO> authPostDTOList);

    PostResponseDTO toDTO(AuthPostDO authPostDO);
}
