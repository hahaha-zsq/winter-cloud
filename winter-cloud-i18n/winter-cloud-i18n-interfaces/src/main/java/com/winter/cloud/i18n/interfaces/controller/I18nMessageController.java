package com.winter.cloud.i18n.interfaces.controller;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/i18nMessage")
public class I18nMessageController implements I18nMessageFacade {
    private final I18nMessageAppService i18nMessageAppService;

    @PostMapping("/getI18nMessageInfo")
    @Override
    public Response<List<I18nMessageDTO>> getI18nMessageInfo(@RequestBody @Validated I18nMessageQuery query) {
        List<I18nMessageDTO> data= i18nMessageAppService.getI18nMessageInfo(query);
        return Response.ok(data);
    }

}
