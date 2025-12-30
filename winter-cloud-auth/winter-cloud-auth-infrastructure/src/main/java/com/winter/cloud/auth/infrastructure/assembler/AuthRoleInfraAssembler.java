package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthRoleInfraAssembler {

    // PO 转 Entity
    AuthRoleDO toDO(AuthRolePO authRolePO);

    // Entity 转 PO
    AuthRolePO toPO(AuthRoleDO authRoleDO);

    // 也可以定义 List 的转换
    List<AuthRoleDO> toDOList(List<AuthRolePO> authRolePOList);

    List<RoleResponseDTO> toDTOList(List<AuthRolePO> authRolePOList);
}