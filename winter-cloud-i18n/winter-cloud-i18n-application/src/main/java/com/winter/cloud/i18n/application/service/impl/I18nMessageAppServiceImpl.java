package com.winter.cloud.i18n.application.service.impl;

import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.application.assembler.I18nMessageAppAssembler;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class I18nMessageAppServiceImpl implements I18nMessageAppService {
    private final I18nMessageRepository i18nMessageRepository;
    private final I18nMessageAppAssembler i18nMessageAppAssembler;

    @Override
    public List<I18nMessageDTO> getI18nMessageInfo(I18nMessageQuery query) {
        List<I18nMessageDO> data = i18nMessageRepository.getI18nMessageInfo(query);
        return i18nMessageAppAssembler.toI18nMessageDTOList(data);
    }
}