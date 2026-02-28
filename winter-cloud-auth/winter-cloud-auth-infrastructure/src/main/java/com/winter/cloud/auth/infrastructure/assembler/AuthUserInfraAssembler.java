package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthUserInfraAssembler {

    // PO 转 Entity
    AuthUserDO toDO(AuthUserPO authUserPO);

    UserRegisterCommand toUserRegisterCommand(AuthUserDO authUserDO);

    // Entity 转 PO
    AuthUserPO toPO(AuthUserDO authUserDO);

    // 也可以定义 List 的转换
    List<AuthUserDO> toDOList(List<AuthUserPO> authUserPOList);
}