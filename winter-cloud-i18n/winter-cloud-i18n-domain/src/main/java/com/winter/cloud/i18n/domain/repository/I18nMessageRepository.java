package com.winter.cloud.i18n.domain.repository;

import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.zsq.i18n.service.I18nMessageService;

import java.util.List;

/**
 * 国际化消息仓储接口
 * 继承 Winter I18n 框架的 I18nMessageService 接口
 * 提供国际化消息获取和查询功能
 */
public interface I18nMessageRepository extends I18nMessageService {

    List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query);
    String findMessageByKeyAndLocale(String messageKey, String locale);
    String getMessage(String messageKey);
    String getMessage(String messageKey,Object[] args);
    String getMessage(String messageKey, Object[] args, String defaultMessage);
}
