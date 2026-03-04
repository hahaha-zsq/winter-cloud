package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.AssignResourcesCommand;
import com.winter.cloud.auth.api.dto.command.UpsertRoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.auth.application.service.AuthRoleAppService;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理控制层，提供角色相关的RESTful接口。
 * <p>
 * 包含角色的查询、新增、修改、删除、导入导出、分配菜单权限等功能。
 *
 * @author winter-cloud
 * @version 1.0.0
 * @since 1.0.0
 * @see com.winter.cloud.auth.application.service.AuthRoleAppService
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/role")
@Validated
public class AuthRoleController {
    private final AuthRoleAppService authRoleAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 分页查询角色列表。
     * <p>
     * 支持根据角色名称、状态等条件进行分页查询，返回符合条件的角色数据。
     *
     * @param roleQuery 分页查询参数，包含分页信息和过滤条件
     * @return 分页后的角色列表
     * @throws IllegalArgumentException 如果分页参数无效
     */
    @PostMapping("/rolePage")
    public Response<PageDTO<RoleResponseDTO>> rolePage(@RequestBody RoleQuery roleQuery) {
        PageDTO<RoleResponseDTO> data = authRoleAppService.rolePage(roleQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 根据动态条件查询角色列表。
     * <p>
     * 支持根据角色状态、角色名称等条件进行模糊查询和动态过滤，返回符合条件的角色列表。
     *
     * @param roleQuery 角色查询条件，包含分页信息和过滤条件
     * @return 符合条件的角色列表
     * @throws IllegalArgumentException 如果查询条件无效
     */
    @PostMapping("/roleDynamicQueryList")
    public Response<List<RoleResponseDTO>> roleDynamicQueryList(@RequestBody @Validated RoleQuery roleQuery) {
        List<RoleResponseDTO> data = authRoleAppService.roleDynamicQueryList(roleQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);

    }

    /**
     * 新增角色信息。
     * <p>
     * 创建一个新的角色记录，角色名称必须唯一。
     *
     * @param command 角色信息，包含角色名称、状态、排序等属性
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果角色信息验证失败
     * @throws com.winter.cloud.common.exception.BusinessException 如果角色名称已存在
     */
    @PostMapping("/roleSave")
    public Response<Boolean> roleSave(@RequestBody @Validated(UpsertRoleCommand.Save.class) UpsertRoleCommand command) {
        Boolean data = authRoleAppService.roleSave(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 修改角色信息。
     * <p>
     * 更新指定角色的相关信息，角色名称必须唯一。
     *
     * @param command 角色信息，包含角色ID、名称、状态、排序等属性
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果角色信息验证失败
     * @throws com.winter.cloud.common.exception.BusinessException 如果角色不存在或名称已存在
     */
    @PutMapping("/roleUpdate")
    public Response<Boolean> roleUpdate(@RequestBody @Validated(UpsertRoleCommand.Update.class) UpsertRoleCommand command) {
        Boolean data = authRoleAppService.roleUpdate(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 删除角色信息。
     * <p>
     * 批量删除指定的角色。
     *
     * @param roleIds 角色ID集合，不能为空
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果ID列表为空
     * @throws com.winter.cloud.common.exception.BusinessException 如果角色关联了用户
     */
    @DeleteMapping("/roleDelete")
    public Response<Boolean> roleDelete(@RequestBody @Valid @NotEmpty(message = "{delete.data.notEmpty}") List<Long> roleIds) {
        Boolean data = authRoleAppService.roleDelete(roleIds);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()), data);
    }

    /**
     * 导出角色数据到Excel文件。
     * <p>
     * 根据查询条件导出符合条件的角色数据为Excel格式。
     *
     * @param response  HTTP响应对象，用于设置响应头和输出流
     * @param roleQuery 导出数据的查询条件
     * @throws IOException 如果写入Excel文件失败
     * @see #roleExportExcelTemplate(HttpServletResponse)
     */
    @PostMapping(value = "/roleExportExcel")
    public void roleExportExcel(HttpServletResponse response,@RequestBody RoleQuery roleQuery ) {
        authRoleAppService.roleExportExcel(response,roleQuery);
    }

    /**
     * 导出角色Excel导入模板。
     * <p>
     * 下载用于批量导入角色的Excel模板文件。
     *
     * @param response HTTP响应对象，用于设置响应头和输出流
     * @throws IOException 如果写入Excel文件失败
     * @see #roleExportExcel(HttpServletResponse, RoleQuery)
     */
    @PostMapping(value = "/roleExportExcelTemplate")
    public void roleExportExcelTemplate(HttpServletResponse response) {
        authRoleAppService.roleExportExcelTemplate(response);
    }

    /**
     * 从Excel文件导入角色数据。
     * <p>
     * 解析Excel文件中的角色数据并进行批量导入
     *
     * @param response HTTP响应对象，用于返回导入结果
     * @param file     Excel文件，包含角色数据
     * @throws IOException 如果读取Excel文件失败
     * @throws IllegalArgumentException 如果文件格式不正确
     */
    @PostMapping(value = "/roleImportExcel")
    public void roleImportExcel(HttpServletResponse response, @RequestParam(value = "file") MultipartFile file) throws IOException {
        authRoleAppService.roleImportExcel(response,file);
    }



    /**
     * 分配菜单权限。
     * <p>
     * 为指定角色分配菜单权限，更新角色的菜单关联关系。
     *
     * @param command 分配资源命令，包含角色ID和菜单ID列表
     * @return 操作结果
     * @throws IllegalArgumentException 如果参数验证失败
     * @throws com.winter.cloud.common.exception.BusinessException 如果角色不存在
     */
    @PostMapping("/assignMenuPermissions")
    public Response<Void> assignMenuPermissions(@RequestBody @Validated AssignResourcesCommand command ) {
        authRoleAppService.assignMenuPermissions(command.getRoleId(), command.getMenuIds());
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),null);
    }
}
