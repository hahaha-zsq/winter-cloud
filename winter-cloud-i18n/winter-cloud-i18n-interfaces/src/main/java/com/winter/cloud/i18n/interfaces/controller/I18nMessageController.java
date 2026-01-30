package com.winter.cloud.i18n.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.dto.command.TranslateCommand;
import com.winter.cloud.i18n.api.dto.command.UpsertI18NCommand;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.dto.response.TranslateDTO;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


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
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @PostMapping("/getI18nMessageInfo")
    @Override
    public Response<List<I18nMessageDTO>> getI18nMessageInfo(@RequestBody @Validated I18nMessageQuery query) {
        List<I18nMessageDTO> data = i18nMessageAppService.getI18nMessageInfo(query);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @Override
    public Response<String> findMessageByKeyAndLocale(String messageKey, String locale) {
        String data = i18nMessageAppService.findMessageByKeyAndLocale(messageKey, locale);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 翻译
     *
     * @param translateCommand 翻译参数
     * @return 翻译结果
     */
    @PostMapping("/translate")
    @Override
    public Response<TranslateDTO> translate(@RequestBody @Validated TranslateCommand translateCommand) throws ExecutionException, InterruptedException {
        TranslateDTO data = i18nMessageAppService.translate(translateCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 分页查询
     *
     * @param i18nMessageQuery 查询条件
     * @return 查询结果
     */
    @PostMapping("/i18nPage")
    public Response<PageDTO<I18nMessageDTO>> i18nPage(@RequestBody I18nMessageQuery i18nMessageQuery) {
        PageDTO<I18nMessageDTO> data = i18nMessageAppService.i18nPage(i18nMessageQuery);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                data
        );
    }

    /**
     * 新增国际化信息
     *
     * @param upsertI18NCommand 保存参数
     * @return 保存结果
     */
    @PostMapping("/i18nSave")
    public Response<Boolean> i18nSave(@RequestBody @Validated UpsertI18NCommand upsertI18NCommand) {
        Boolean data = i18nMessageAppService.i18nSave(upsertI18NCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 修改国际化信息
     *
     * @param upsertI18NCommand 保存参数
     * @return 保存结果
     */
    @PostMapping("/i18nUpdate")
    public Response<Boolean> i18nUpdate(@RequestBody @Validated(UpsertI18NCommand.Update.class) UpsertI18NCommand upsertI18NCommand) {
        Boolean data = i18nMessageAppService.i18nUpdate(upsertI18NCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 删除国际化信息
     *
     * @param ids 要删除的id
     * @return 删除结果
     */
    @PostMapping("/i18nDelete")
    public Response<Boolean> i18nDelete(@RequestBody @Valid @NotEmpty(message = "要删除的数据不能为空") List<Long> ids) {
        Boolean data = i18nMessageAppService.i18nDelete(ids);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }



    @Override
    public String getMessage(String messageKey) {
        return i18nMessageAppService.getMessage(messageKey);
    }

    @Override
    public String getMessage(String messageKey, Locale locale) {
        return i18nMessageAppService.getMessage(messageKey, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args) {
        return i18nMessageAppService.getMessage(messageKey, args);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return i18nMessageAppService.getMessage(messageKey, args, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage) {
        return i18nMessageAppService.getMessage(messageKey, args, defaultMessage);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        return i18nMessageAppService.getMessage(messageKey, args, locale);
    }


}
