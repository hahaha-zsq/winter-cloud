package com.winter.cloud.i18n.application.service;


import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.i18n.api.dto.command.TranslateCommand;
import com.winter.cloud.i18n.api.dto.command.UpsertI18NCommand;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.dto.response.TranslateDTO;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public interface I18nMessageAppService {

    List<I18nMessageDTO> getI18nMessageInfo(I18nMessageQuery query);

    String findMessageByKeyAndLocale(String messageKey, String locale);

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

    TranslateDTO translate(TranslateCommand translateCommand) throws ExecutionException, InterruptedException;

    PageDTO<I18nMessageDTO> i18nPage(I18nMessageQuery i18nMessageQuery);

    Boolean i18nSave(UpsertI18NCommand upsertI18NCommand);

    Boolean i18nUpdate(UpsertI18NCommand upsertI18NCommand);

    Boolean i18nDelete(@Valid @NotEmpty(message = "要删除的数据不能为空") List<Long> ids);
}
