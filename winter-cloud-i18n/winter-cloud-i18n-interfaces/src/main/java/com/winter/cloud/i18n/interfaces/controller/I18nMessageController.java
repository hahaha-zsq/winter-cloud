package com.winter.cloud.i18n.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.zsq.i18n.template.WinterI18nTemplate;
import com.zsq.i18n.utils.WinterI18nUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 国际化模块
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/i18nMessage")
public class I18nMessageController implements I18nMessageFacade {
    private final I18nMessageAppService i18nMessageAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 根据条件查询国际化信息
     * @param query 查询条件
     * @return 查询结果
     */
    @PostMapping("/getI18nMessageInfo")
    @Override
    public Response<List<I18nMessageDTO>> getI18nMessageInfo(@RequestBody @Validated I18nMessageQuery query) {
        List<I18nMessageDTO> data= i18nMessageAppService.getI18nMessageInfo(query);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);
    }

}
