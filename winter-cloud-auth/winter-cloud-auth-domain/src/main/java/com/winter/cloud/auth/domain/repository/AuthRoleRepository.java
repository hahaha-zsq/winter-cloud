package com.winter.cloud.auth.domain.repository;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.common.response.PageDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 角色仓储接口 (面向领域)
 */
public interface AuthRoleRepository {

    Boolean roleSave(AuthRoleDO aDo);
    // 检查用户名是否存在
    boolean hasDuplicateRole(AuthRoleDO aDo);

    Boolean roleUpdate(AuthRoleDO aDo);

    Boolean roleDelete(List<Long> roleIds);

    PageDTO<AuthRoleDO> rolePage(RoleQuery roleQuery);

    List<AuthRoleDO> selectRoleListByUserId(Long userId, String status);

    List<AuthRoleDO> roleDynamicQueryList(RoleQuery roleQuery);

    void assignMenuPermissions(Long roleId, List<Long> menuIds);

    void roleExportExcel(HttpServletResponse response);

    void roleExportExcelTemplate(HttpServletResponse response);

    void roleImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;
}