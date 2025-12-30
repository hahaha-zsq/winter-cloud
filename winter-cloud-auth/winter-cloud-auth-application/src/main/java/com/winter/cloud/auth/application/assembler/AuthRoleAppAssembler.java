package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.command.RoleCommand;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthRoleAppAssembler {

    AuthRoleDO toDO(RoleCommand command);
}
