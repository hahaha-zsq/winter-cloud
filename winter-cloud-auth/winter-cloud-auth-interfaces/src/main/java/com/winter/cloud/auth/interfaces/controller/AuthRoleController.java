package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.RoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.auth.application.service.AuthRoleAppService;
import com.winter.cloud.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/role")
public class AuthRoleController {
    private final AuthRoleAppService authRoleAppService;

    /**
     * 分页查询角色列表
     *
     * @param roleQuery 分页查询参数
     */
    @PostMapping("/rolePage")
    public Response<PageDTO<RoleResponseDTO>> rolePage(@RequestBody RoleQuery roleQuery) {
        PageDTO<RoleResponseDTO> data = authRoleAppService.rolePage(roleQuery);
        return Response.ok(data);
    }

    /**
     * 新增角色
     *
     * @param command 角色信息
     */
    @PostMapping("/saveRole")
    public Response<Boolean> saveRole(@RequestBody RoleCommand command) {
        Boolean data = authRoleAppService.saveRole(command);
        return Response.ok(data);
    }

    /**
     * 修改角色
     *
     * @param command 角色信息
     */
    @PutMapping("/updateRole")
    public Response<Boolean> updateRole(@RequestBody RoleCommand command) {
        Boolean data = authRoleAppService.updateRole(command);
        return Response.ok(data);
    }

    /**
     * 删除角色
     *
     * @param roleIds 角色id集合
     */
    @DeleteMapping("/deleteRole")
    public Response<Boolean> deleteRole(@RequestBody List<Long> roleIds) {
        Boolean data = authRoleAppService.deleteRole(roleIds);
        return Response.ok(data);
    }
}
