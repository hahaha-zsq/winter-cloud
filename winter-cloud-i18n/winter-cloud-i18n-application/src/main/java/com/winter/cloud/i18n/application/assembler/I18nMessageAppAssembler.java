package com.winter.cloud.i18n.application.assembler;

import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.dto.response.TranslateDTO;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.model.entity.TranslateDO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface I18nMessageAppAssembler {

    List<I18nMessageDTO> toI18nMessageDTOList(List<I18nMessageDO> i18nMessageDOList);

    TranslateDTO toTranslateDTO(TranslateDO translateDO);
}
