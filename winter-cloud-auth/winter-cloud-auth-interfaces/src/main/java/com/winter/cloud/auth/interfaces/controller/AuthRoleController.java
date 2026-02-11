package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.UpsertRoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.auth.application.service.AuthRoleAppService;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/role")
@Validated
@DubboService
public class AuthRoleController {
    private final AuthRoleAppService authRoleAppService;
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    /**
     * 分页查询角色列表
     *
     * @param roleQuery 分页查询参数
     */
    @PostMapping("/rolePage")
    public Response<PageDTO<RoleResponseDTO>> rolePage(@RequestBody RoleQuery roleQuery) {
        PageDTO<RoleResponseDTO> data = authRoleAppService.rolePage(roleQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 根据状态和角色名称获取角色信息
     */
    @PostMapping("/roleDynamicQueryList")
    public Response<List<RoleResponseDTO>> roleDynamicQueryList(@RequestBody @Validated RoleQuery roleQuery) {
        List<RoleResponseDTO> data = authRoleAppService.roleDynamicQueryList(roleQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);

    }

    /**
     * 新增角色
     *
     * @param command 角色信息
     */
    @PostMapping("/roleSave")
    public Response<Boolean> roleSave(@RequestBody @Validated(UpsertRoleCommand.Save.class) UpsertRoleCommand command) {
        Boolean data = authRoleAppService.roleSave(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 修改角色
     *
     * @param command 角色信息
     */
    @PutMapping("/roleUpdate")
    public Response<Boolean> roleUpdate(@RequestBody @Validated(UpsertRoleCommand.Update.class) UpsertRoleCommand command) {
        Boolean data = authRoleAppService.roleUpdate(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 删除角色
     *
     * @param roleIds 角色id集合
     */
    @DeleteMapping("/roleDelete")
    public Response<Boolean> roleDelete(@RequestBody @NotEmpty(message = "{delete.data.notEmpty}") List<Long> roleIds) {
        Boolean data = authRoleAppService.roleDelete(roleIds);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }
    /**
     * 分配菜单权限
     */
    @PostMapping("/assignMenuPermissions")
    public Response<Void> assignMenuPermissions(@RequestParam @NotNull Long roleId, @RequestParam @NotEmpty  List<Long> menuIds) {
        authRoleAppService.assignMenuPermissions(roleId, menuIds);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),null);
    }
}
