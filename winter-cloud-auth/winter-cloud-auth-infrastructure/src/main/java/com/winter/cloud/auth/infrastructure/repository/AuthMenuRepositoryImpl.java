package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.query.MenuQuery;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthMenuDO;
import com.winter.cloud.auth.domain.repository.AuthMenuRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthMenuInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthMenuPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthMenuMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthMenuMpService;
import com.winter.cloud.common.enums.MenuTypeEnum;
import com.winter.cloud.common.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AuthMenuRepositoryImpl implements AuthMenuRepository {
    private final IAuthMenuMpService authMenuMpService;
    private final AuthMenuMapper authMenuMapper;
    private final AuthMenuInfraAssembler authMenuInfraAssembler;

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

    @Override
    public List<AuthMenuDO> getMenuList(MenuQuery menuQuery) {
        List<AuthMenuPO> list = authMenuMpService.list(new LambdaQueryWrapper<AuthMenuPO>()
                .eq(ObjectUtil.isNotEmpty(menuQuery.getId()), AuthMenuPO::getId, menuQuery.getId())
                .like(ObjectUtil.isNotEmpty(menuQuery.getMenuName()), AuthMenuPO::getMenuName, menuQuery.getMenuName())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getStatus()), AuthMenuPO::getStatus, menuQuery.getStatus())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getMenuType()), AuthMenuPO::getMenuType, menuQuery.getMenuType())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getVisible()), AuthMenuPO::getVisible, menuQuery.getVisible())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getPerms()), AuthMenuPO::getPerms, menuQuery.getPerms())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getFrame()), AuthMenuPO::getFrame, menuQuery.getFrame())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getPath()), AuthMenuPO::getPath, menuQuery.getPath())
        );
        return authMenuInfraAssembler.toDOList(list);
    }

    @Override
    public List<AuthMenuDO> listByIds(Set<Long> allIds) {
        if (CollUtil.isEmpty(allIds)) {
            return List.of();
        }
        List<AuthMenuPO> list = authMenuMpService.list(new LambdaQueryWrapper<AuthMenuPO>().in(AuthMenuPO::getId, allIds));
        return authMenuInfraAssembler.toDOList(list);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean menuSave(AuthMenuDO authMenuDO) {
        AuthMenuPO po = authMenuInfraAssembler.toPO(authMenuDO);
        return authMenuMpService.save(po);
    }

}
