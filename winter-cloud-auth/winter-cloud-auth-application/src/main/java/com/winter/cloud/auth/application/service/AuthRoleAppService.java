package com.winter.cloud.auth.application.service;


import com.winter.cloud.auth.api.dto.command.UpsertRoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.common.response.PageDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

public interface AuthRoleAppService {

    /**
     * 保存角色
     *
     * @param command 角色保存命令
     * @return 是否保存成功
     */
    Boolean roleSave(UpsertRoleCommand command);

    /**
     * 更新角色
     *
     * @param command 角色更新命令
     * @return 是否更新成功
     */
    Boolean roleUpdate(UpsertRoleCommand command);

    /**
     * 删除角色
     *
     * @param roleIds 角色id集合
     * @return 是否删除成功
     */
    Boolean roleDelete(List<Long> roleIds);

    /**
     * 分页查询角色
     *
     * @param roleQuery 角色查询参数
     * @return 角色分页数据
     */
    PageDTO<RoleResponseDTO> rolePage(RoleQuery roleQuery);


    List<RoleResponseDTO> roleDynamicQueryList(RoleQuery roleQuery);

    void assignMenuPermissions(@NotNull Long roleId, @NotEmpty List<Long> menuIds);

    void roleExportExcel(HttpServletResponse response);

    void roleExportExcelTemplate(HttpServletResponse response);

    void roleImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;
}