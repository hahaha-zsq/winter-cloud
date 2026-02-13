package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.command.UpsertMenuCommand;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthMenuDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMenuAppAssembler {

    List<MenuResponseDTO> toDTOList(List<AuthMenuDO> authMenuDOList);
    AuthMenuDO toDO(UpsertMenuCommand command);


}
