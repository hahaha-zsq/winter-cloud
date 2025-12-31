package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.infrastructure.entity.AuthMenuPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单（权限）表 Mapper 接口
 * 继承 BaseMapper 后自动拥有 CRUD 能力
 */
public interface AuthMenuMapper extends BaseMapper<AuthMenuPO> {
    List<MenuResponseDTO> selectMenuListByRoleIdList(@Param("roleIdList") List<Long> roleIdList,@Param("status") String status);
    // 如果有复杂的自定义 SQL，可以在这里定义方法并在 XML 中实现
}