package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.UpsertMenuCommand;
import com.winter.cloud.auth.api.dto.query.MenuQuery;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.application.service.AuthMenuAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜单
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/menu")
@Validated
public class AuthMenuController {
    private final AuthMenuAppService authMenuAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 获取动态路由(只需要查询对应的菜单类型为m的即可，不需要递归)
     *
     * @param id 用户ID
     * @return 菜单列表
     */
    @GetMapping("/getDynamicRouting")
    public Response<List<MenuResponseDTO>> getDynamicRouting(@NotNull @RequestParam(value = "id") Long id) {
        List<MenuResponseDTO> dynamicRouting = authMenuAppService.getDynamicRouting(id);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),dynamicRouting);
    }

    /**
     * 查询menu(菜单是tree类型的，父子菜单)
     *
     * @param menuQuery 查询条件
     * @return 菜单列表
     */
    @PostMapping("/menuTree")
    public Response<List<MenuResponseDTO>> menuTree(@RequestBody MenuQuery menuQuery) {
        // 查询menu(菜单是tree类型的，父子菜单)
        List<MenuResponseDTO> data = authMenuAppService.menuTree(menuQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),data);
    }
    /**
     * 新增资源
     * @param command 新增
     * @return 是否新增成功
     */
    @PostMapping("/menuSave")
    public Response<Boolean> menuSave(@RequestBody @Validated(UpsertMenuCommand.Save.class) UpsertMenuCommand command){
        boolean data = authMenuAppService.menuSave(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),data);
    }

    /**
     * 新增/编辑时需要的可选的父级菜单
     * @param userId 用户ID
     * @return 父菜单列表
     */
    @GetMapping("/parentMenuTree")
    public Response<List<MenuResponseDTO>> parentMenuTree(@RequestParam Long userId) {
        List<MenuResponseDTO> data = authMenuAppService.getMenu(userId);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),data);
    }

}
