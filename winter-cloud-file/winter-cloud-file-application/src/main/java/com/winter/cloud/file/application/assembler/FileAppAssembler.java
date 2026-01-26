package com.winter.cloud.file.application.assembler;


import com.winter.cloud.file.api.dto.response.FileCheckDTO;
import com.winter.cloud.file.domain.model.entity.TaskInfoDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileAppAssembler {
   FileCheckDTO toFileCheckDTO(TaskInfoDO taskInfoDO);
}
