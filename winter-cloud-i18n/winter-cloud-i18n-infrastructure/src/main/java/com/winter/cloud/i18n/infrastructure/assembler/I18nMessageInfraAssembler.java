package com.winter.cloud.i18n.infrastructure.assembler;

import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface I18nMessageInfraAssembler {
    List<I18nMessageDO> toDOList(List<I18nMessagePO> i18nMessagePOList);
}