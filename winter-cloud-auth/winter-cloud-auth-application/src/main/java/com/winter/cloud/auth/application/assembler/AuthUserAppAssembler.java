package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthUserAppAssembler {
    /**
     * 注册命令 -> 领域实体
     * 对应 register 方法中的 BeanUtil.copyProperties
     */
    AuthUserDO toDO(UserRegisterCommand command);
}
