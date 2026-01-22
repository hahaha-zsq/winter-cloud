package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色表 Mapper 接口
 * 继承 BaseMapper 后自动拥有 CRUD 能力
 */
public interface AuthRoleMapper extends BaseMapper<AuthRolePO> {
    IPage<AuthRolePO> rolePage(Page<AuthRolePO> page, @Param("roleQuery") RoleQuery roleQuery);

    List<AuthRolePO> selectRoleIdListByUserId(@Param("userId") Long userId,@Param("status") String status);
}