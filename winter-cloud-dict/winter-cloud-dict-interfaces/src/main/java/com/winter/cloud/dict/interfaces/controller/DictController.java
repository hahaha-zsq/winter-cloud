package com.winter.cloud.dict.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.winter.cloud.dict.application.service.DictAppService;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dict")
@DubboService
public class DictController implements DictFacade {
    private final DictAppService dictAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 根据查询条件获取字典数据
     */
    @PostMapping("/getDictDataByType")
    @Override
    public Response<Map<String, List<DictDataDTO>>> getDictDataByType(@RequestBody DictCommand dictCommand) {
        List<DictDataDTO> data = dictAppService.getDictDataByType(dictCommand.getDictType(), dictCommand.getStatus());
        Map<String, List<DictDataDTO>> collect = data.stream().collect(Collectors.groupingBy(DictDataDTO::getDictLabel));
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), collect);
    }

    /**
     * 根据查询条件获取字典数据
     */
    @PostMapping("/dictValueDynamicQueryList")
    @Override
    public Response<List<DictDataDTO>> dictValueDynamicQueryList(@RequestBody DictQuery dictQuery) {
        List<DictDataDTO> data = dictAppService.dictValueDynamicQueryList(dictQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }
}
