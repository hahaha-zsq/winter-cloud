package com.winter.cloud.auth.domain.repository;


import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;

import java.util.List;

/**
 * 菜单仓储接口 (面向领域)
 */
public interface AuthMenuRepository {

    List<MenuResponseDTO> selectMenuListByRoleIdList(List<Long> roleIdList,String status);
}