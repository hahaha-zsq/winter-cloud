package com.winter.cloud.dict.starter.config;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.winter.cloud.i18n.client.I18nMessageInfo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
public class I18nInfoProvider implements I18nMessageInfo {
    /**
     * Dubbo 远程认证服务
     */
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;


    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        Response<String> messageByKeyAndLocale = i18nMessageFacade.findMessageByKeyAndLocale(messageKey, locale);
        return messageByKeyAndLocale.getData();
    }
}
