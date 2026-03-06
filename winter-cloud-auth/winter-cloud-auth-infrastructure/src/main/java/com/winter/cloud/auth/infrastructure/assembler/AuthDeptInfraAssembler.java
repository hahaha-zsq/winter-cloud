package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthDeptInfraAssembler {

    AuthDeptDO toDO(AuthDeptPO authDeptPO);

    AuthDeptPO toPO(AuthDeptDO authDeptDO);

    List<AuthDeptDO> toDOList(List<AuthDeptPO> authDeptPOList);

    List<DeptResponseDTO> toDTOList(List<AuthDeptPO> authDeptPOList);
}