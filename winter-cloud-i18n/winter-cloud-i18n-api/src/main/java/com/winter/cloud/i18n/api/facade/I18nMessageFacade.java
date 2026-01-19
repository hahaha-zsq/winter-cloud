package com.winter.cloud.i18n.api.facade;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;

import java.util.List;

public interface I18nMessageFacade {

    /**
     * 根据查询条件查询国际化消息信息
     */
    Response<List<I18nMessageDTO>>  getI18nMessageInfo(I18nMessageQuery query);
    Response<String>  findMessageByKeyAndLocale(String messageKey, String locale);
}
