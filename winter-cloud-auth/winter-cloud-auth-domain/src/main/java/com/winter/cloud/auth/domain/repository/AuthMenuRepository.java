package com.winter.cloud.auth.domain.repository;


import cn.hutool.core.lang.Opt;
import com.winter.cloud.auth.api.dto.query.MenuQuery;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthMenuDO;

import java.util.List;

/**
 * 菜单仓储接口 (面向领域)
 */
public interface AuthMenuRepository {

    /**
     * 根据角色 ID 列表查询权限
     *
     * @param roleIdList 角色 ID 列表
     * @param status     状态
     * @return 菜单列表
     */
    List<MenuResponseDTO> selectMenuListByRoleIdList(List<Long> roleIdList,String status);

    /**
     * 根据用户id获取目录和菜单（层次结构）
     *
     * @param userId 用户 ID
     * @return 菜单列表
     */
    List<MenuResponseDTO> getMenu(Long userId);

    List<MenuResponseDTO> getDynamicRouting(Long id);
    /**
     * 根据查询条件获取菜单列表
     *
     * @param menuQuery 查询条件
     * @return 菜单列表
     */
    List<AuthMenuDO> getMenuList(MenuQuery menuQuery);
}