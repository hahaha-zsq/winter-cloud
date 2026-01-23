package com.winter.cloud.i18n.api.facade;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;

import java.util.List;
import java.util.Locale;

public interface I18nMessageFacade {

    /**
     * 根据查询条件查询国际化消息信息
     */
    Response<List<I18nMessageDTO>>  getI18nMessageInfo(I18nMessageQuery query);
    Response<String>  findMessageByKeyAndLocale(String messageKey, String locale);
    String getMessage(String messageKey);
    /**
     * 获取无参国际化消息
     */
    String getMessage(String messageKey, Locale locale);
    String getMessage(String messageKey, Object[] args);

    /**
     * 获取带参数国际化消息
     * @param args 占位符参数，如 {0}, {1}
     */
    String getMessage(String messageKey, Object[] args, Locale locale);

    String getMessage(String messageKey, Object[] args, String defaultMessage);
    /**
     * 获取全功能国际化消息（带参数、带默认值）
     * @param defaultMessage 当查不到消息时的兜底返回
     */
    String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale);


}
