package com.winter.cloud.i18n.domain.repository;

import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.client.I18nMessageInfo;

import java.util.List;

/**
 * 国际化消息仓储接口
 * 继承 Winter I18n 框架的 I18nMessageService 接口
 * 提供国际化消息获取和查询功能
 */
public interface I18nMessageRepository extends I18nMessageInfo {

    List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query);
}
