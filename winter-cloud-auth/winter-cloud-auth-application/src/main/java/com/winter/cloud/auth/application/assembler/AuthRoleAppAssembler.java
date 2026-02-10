package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.command.UpsertRoleCommand;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthRoleAppAssembler {

    AuthRoleDO toDO(UpsertRoleCommand command);

    List<RoleResponseDTO> toDTOList(List<AuthRoleDO> authRoleDOList);
}
