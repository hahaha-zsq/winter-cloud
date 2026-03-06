package com.winter.cloud.dict.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.command.UpsertDictDataCommand;
import com.winter.cloud.dict.api.dto.command.UpsertDictTypeCommand;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.api.dto.query.DictTypeQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.winter.cloud.dict.application.service.DictAppService;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dict")
@Validated
@DubboService
public class DictController implements DictFacade {
    private final DictAppService dictAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    @PostMapping("/getDictDataByType")
    @Override
    public Response<Map<String, List<DictDataDTO>>> getDictDataByType(@RequestBody DictCommand dictCommand) {
        List<DictDataDTO> data = dictAppService.getDictDataByType(dictCommand.getDictType(), dictCommand.getStatus());
        Map<String, List<DictDataDTO>> collect = data.stream().collect(Collectors.groupingBy(DictDataDTO::getDictLabel));
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), collect);
    }

    @PostMapping("/dictValueDynamicQueryList")
    @Override
    public Response<List<DictDataDTO>> dictValueDynamicQueryList(@RequestBody DictDataQuery dictQuery) {
        List<DictDataDTO> data = dictAppService.dictValueDynamicQueryList(dictQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PostMapping("/dictTypeList")
    public Response<List<DictTypeDTO>> dictTypeList(@RequestBody DictTypeQuery dictTypeQuery) {
        List<DictTypeDTO> data = dictAppService.dictTypeList(dictTypeQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PostMapping("/dictTypePage")
    public Response<PageDTO<DictTypeDTO>> dictTypePage(@RequestBody DictTypeQuery dictTypeQuery) {
        PageDTO<DictTypeDTO> data = dictAppService.dictTypePage(dictTypeQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictTypeSave')")
    @PostMapping("/dictTypeSave")
    public Response<Boolean> dictTypeSave(@RequestBody @Validated(UpsertDictTypeCommand.Save.class) UpsertDictTypeCommand upsertDictTypeCommand) {
        Boolean data = dictAppService.dictTypeSave(upsertDictTypeCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictTypeUpdate')")
    @PutMapping("/dictTypeUpdate")
    public Response<Boolean> dictTypeUpdate(@RequestBody @Validated(UpsertDictTypeCommand.Update.class) UpsertDictTypeCommand upsertDictTypeCommand) {
        Boolean data = dictAppService.dictTypeUpdate(upsertDictTypeCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictTypeDelete')")
    @DeleteMapping("/dictTypeDelete")
    public Response<Boolean> dictTypeDelete(@RequestBody @NotEmpty(message = "{delete.data.notEmpty}") List<Long> ids) {
        Boolean data = dictAppService.dictTypeDelete(ids);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PostMapping("/dictDataPage")
    public Response<PageDTO<DictDataDTO>> dictDataPage(@RequestBody DictDataQuery dictQuery) {
        PageDTO<DictDataDTO> data = dictAppService.dictDataPage(dictQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictDataSave')")
    @PostMapping("/dictDataSave")
    public Response<Boolean> dictDataSave(@RequestBody @Validated(UpsertDictDataCommand.Save.class) List<UpsertDictDataCommand>  upsertDictDataCommandList) {
        Boolean data = dictAppService.dictDataSave(upsertDictDataCommandList);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictDataUpdate')")
    @PutMapping("/dictDataUpdate")
    public Response<Boolean> dictDataUpdate(@RequestBody @Validated(UpsertDictDataCommand.Update.class) UpsertDictDataCommand upsertDictDataCommand) {
        Boolean data = dictAppService.dictDataUpdate(upsertDictDataCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dict:dictDataDelete')")
    @DeleteMapping("/dictDataDelete")
    public Response<Boolean> dictDataDelete(@RequestBody @NotEmpty(message = "{delete.data.notEmpty}") List<Long> ids) {
        Boolean data = dictAppService.dictDataDelete(ids);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

}
