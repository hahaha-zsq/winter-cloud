package com.winter.cloud.auth.application.assembler;

import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthDeptAppAssembler {


    List<DeptResponseDTO> toDTOList(List<AuthDeptDO> authDeptDOList);
    DeptResponseDTO toDTO(AuthDeptDO authDeptDO);

}
