package com.winter.cloud.i18n.application.service;


import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;

import java.util.List;

public interface I18nMessageAppService {

    List<I18nMessageDTO> getI18nMessageInfo(I18nMessageQuery query);
}
