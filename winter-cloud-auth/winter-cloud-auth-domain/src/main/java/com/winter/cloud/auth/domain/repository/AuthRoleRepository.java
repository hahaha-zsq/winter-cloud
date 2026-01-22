package com.winter.cloud.auth.domain.repository;


import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.common.response.PageDTO;

import java.util.List;

/**
 * 角色仓储接口 (面向领域)
 */
public interface AuthRoleRepository {

    Boolean saveRole(AuthRoleDO aDo);
    // 检查用户名是否存在
    boolean hasDuplicateRole(AuthRoleDO aDo);

    Boolean updateRole(AuthRoleDO aDo);

    Boolean deleteRole(List<Long> roleIds);

    PageDTO<AuthRoleDO> rolePage(RoleQuery roleQuery);

    List<AuthRoleDO> selectRoleListByUserId(Long userId, String status);

    List<AuthRoleDO> getAllRoleInfo(String roleName, String status);
}