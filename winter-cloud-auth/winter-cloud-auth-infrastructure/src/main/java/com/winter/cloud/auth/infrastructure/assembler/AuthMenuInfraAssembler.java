package com.winter.cloud.auth.infrastructure.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMenuInfraAssembler {

}