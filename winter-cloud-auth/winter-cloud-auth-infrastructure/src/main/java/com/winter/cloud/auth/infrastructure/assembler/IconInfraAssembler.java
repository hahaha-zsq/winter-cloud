package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.model.entity.IconTypeDO;
import com.winter.cloud.auth.domain.model.entity.IconValueDO;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.entity.IconTypePO;
import com.winter.cloud.auth.infrastructure.entity.IconValuePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IconInfraAssembler {
    IconTypePO toPO(IconTypeDO iconTypeDO);
    IconValuePO toPO(IconValueDO iconValueDO);
    List<IconValuePO> toPOList(List<IconValueDO> iconValueDOList);
}