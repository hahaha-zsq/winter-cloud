package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.UpsertDeptCommand;
import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.application.service.AuthDeptAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 部门接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dept")
public class AuthDeptController {
    private final AuthDeptAppService authDeptAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 查询所有部门（递归）
     */
    @PostMapping("/selectAllRecursionDept")
    public Response<List<DeptResponseDTO>> selectAllRecursionDept(@RequestBody @Validated DeptQuery deptQuery) {
        List<DeptResponseDTO> data = authDeptAppService.selectAllRecursionDept(deptQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);

    }
    @PostMapping("/deptTree")
    public Response<List<DeptResponseDTO>> deptTree(@RequestBody DeptQuery deptQuery) {
        // 查询menu(菜单是tree类型的，父子菜单)
        List<DeptResponseDTO> data = authDeptAppService.deptTree(deptQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);
    }

    @PreAuthorize("hasAuthority('sys:dept:deptSave')")
    @PostMapping("/deptSave")
    public Response<Boolean> deptSave(@RequestBody @Validated(UpsertDeptCommand.Save.class) UpsertDeptCommand command) {
        Boolean data = authDeptAppService.deptSave(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dept:deptUpdate')")
    @PutMapping("/deptUpdate")
    public Response<Boolean> deptUpdate(@RequestBody @Validated(UpsertDeptCommand.Update.class) UpsertDeptCommand command) {
        Boolean data = authDeptAppService.deptUpdate(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    @PreAuthorize("hasAuthority('sys:dept:deptDelete')")
    @DeleteMapping("/deptDelete")
    public Response<Boolean> deptDelete(@RequestParam("id") @NotNull Long id) {
        Boolean data = authDeptAppService.deptDelete(id);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }
}
