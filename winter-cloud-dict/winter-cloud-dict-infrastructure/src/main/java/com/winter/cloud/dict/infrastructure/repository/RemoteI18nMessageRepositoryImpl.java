package com.winter.cloud.dict.infrastructure.repository;

import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.zsq.i18n.service.I18nMessageService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Locale;

/**
 * 远程国际化消息服务适配器
 * <p>
 * 核心作用：将本地的方法调用，转发为 Dubbo 远程调用。
 * 解决了 "CustomMessageSource 需要数据" 和 "数据在远程服务" 之间的矛盾。主要是为了解决validation的占位符格式化
 */
@Repository
@Primary // 标记为首选 Bean，覆盖 Starter 里的 DefaultI18nMessageService
public class RemoteI18nMessageRepositoryImpl implements I18nMessageService {

    // 注入 Dubbo 远程接口 (请确保引入了 winter-cloud-i18n-api 依赖)
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    @Override
    public String getMessage(String messageKey, Locale locale) {
        return getMessage(messageKey,null,locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return getMessage(messageKey, args,"", locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        return i18nMessageFacade.getMessage(messageKey, args, defaultMessage, locale);
    }
}