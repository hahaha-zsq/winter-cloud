package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.domain.model.entity.AuthMenuDO;
import com.winter.cloud.auth.infrastructure.entity.AuthMenuPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMenuInfraAssembler {
    List<AuthMenuDO> toDOList(List<AuthMenuPO> authMenuPOList);

}