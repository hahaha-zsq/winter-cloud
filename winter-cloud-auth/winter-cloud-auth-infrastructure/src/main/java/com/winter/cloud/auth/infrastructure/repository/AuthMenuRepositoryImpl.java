package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.domain.repository.AuthMenuRepository;
import com.winter.cloud.auth.infrastructure.mapper.AuthMenuMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthMenuMpService;
import com.winter.cloud.common.enums.MenuTypeEnum;
import com.winter.cloud.common.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuthMenuRepositoryImpl implements AuthMenuRepository {
    private final IAuthMenuMpService authMenuMpService;
    private final AuthMenuMapper authMenuMapper;


    @Override
    public List<MenuResponseDTO> selectMenuListByRoleIdList(List<Long> roleIdList, String status) {
        if (ObjectUtil.isNotEmpty(roleIdList)) {
            return authMenuMapper.selectMenuListByRoleIdList(roleIdList, status);
        }
        return List.of();
    }

    @Override
    public List<MenuResponseDTO> getMenu(Long id) {
        if (ObjectUtil.isNotEmpty(id)) {
            return authMenuMapper.getMenu(id,"", CollUtil.toList(MenuTypeEnum.MENU.getCode(),MenuTypeEnum.DIR.getCode()));
        }
        return List.of();
    }


    @Override
    public List<MenuResponseDTO> getDynamicRouting(Long id) {
        if (ObjectUtil.isNotEmpty(id)) {
            // 获取所有启用的菜单,只需要菜单，菜单就是路由，不需要递归（status只影响路由存不存在，visible只影响菜单在页面上显示情况）
            return authMenuMapper.getMenu(id, StatusEnum.ENABLE.getCode(), CollUtil.toList(MenuTypeEnum.MENU.getCode()));
        }
        return List.of();
    }
}
