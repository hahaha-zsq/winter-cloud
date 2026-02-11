package com.winter.cloud.auth.application.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.auth.api.dto.query.MenuQuery;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthMenuAppAssembler;
import com.winter.cloud.auth.application.service.AuthMenuAppService;
import com.winter.cloud.auth.domain.model.entity.AuthMenuDO;
import com.winter.cloud.auth.domain.repository.AuthMenuRepository;
import com.winter.cloud.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMenuAppServiceImpl implements AuthMenuAppService {
    private final AuthMenuAppAssembler authMenuAppAssembler;
    private final AuthMenuRepository authMenuRepository;
    /**
     * 获取用户菜单树形结构
     * <p>
     * 根据用户ID查询该用户拥有权限的所有菜单，并构建成树形结构返回。
     * 菜单树的根节点为parentId=0的菜单项，每个节点递归包含其所有子菜单。
     * </p>
     *
     * @param userId 用户ID，用于查询该用户有权限访问的菜单列表
     * @return 树形结构的菜单列表，根节点为顶级菜单（parentId=0），每个节点包含children字段存储子菜单
     */
    @Override
    public List<MenuResponseDTO> getMenu(Long userId) {
        // 从repository查询用户的所有菜单权限，使用distinct去重后转换为List
        List<MenuResponseDTO> menu = authMenuRepository.getMenu(userId).stream().distinct().collect(Collectors.toList());
        // 将扁平化的菜单列表构建成树形结构
        return builderMenuTree(menu);
    }

    @Override
    public List<MenuResponseDTO> getDynamicRouting(Long id) {
        return authMenuRepository.getDynamicRouting(id).stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<MenuResponseDTO> menuTree(MenuQuery menuQuery) {
        List<AuthMenuDO> data = authMenuRepository.getMenuList(menuQuery);
        List<String> collect = data.stream().map(AuthMenuDO::getAncestors).filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        // 提取所有菜单的id并去重
        HashSet<String> idList = new HashSet<>();
        collect.forEach(item -> {
            idList.addAll(List.of(item.split(CommonConstants.Delimiter.ENGLISH_COMMA)));
        });
        // todo  构建父子树，所有的子节点都挂载到了同一个父节点对象上

        return authMenuAppAssembler.toDTOList(data);
    }

    /**
     * 构建菜单树形结构
     * <p>
     * 将扁平化的菜单列表转换为树形结构。筛选出所有顶级菜单（parentId=0），
     * 然后为每个顶级菜单递归查找并设置其子菜单。
     * </p>
     *
     * @param menuList 扁平化的菜单列表，包含所有菜单项
     * @return 树形结构的菜单列表，只包含顶级菜单，子菜单通过children字段关联
     */
    private List<MenuResponseDTO> builderMenuTree(List<MenuResponseDTO> menuList) {
        return menuList.stream()
                // 筛选出顶级菜单：parentId为0的菜单项作为树的根节点
                .filter(menu -> menu.getParentId().equals(0L))
                // 为每个顶级菜单递归设置其所有子菜单，构建完整的树形结构
                .map(menu -> menu.setChildren(getChildren(menu, menuList)))
                // 收集处理后的顶级菜单列表并返回
                .collect(Collectors.toList());
    }

    /**
     * 递归获取指定菜单的所有子菜单
     * <p>
     * 从菜单列表中筛选出parentId等于当前菜单ID的所有子菜单，
     * 并递归地为每个子菜单查找其下级子菜单，形成完整的子树结构。
     * </p>
     *
     * @param menu 当前菜单节点，需要为其查找子菜单
     * @param menuVoList 完整的菜单列表，用于查找子菜单
     * @return 当前菜单的所有子菜单列表，每个子菜单也包含其children字段
     */
    private List<MenuResponseDTO> getChildren(MenuResponseDTO menu, List<MenuResponseDTO> menuVoList) {
        return menuVoList.stream()
                // 筛选出所有parentId等于当前菜单ID的菜单项，即当前菜单的直接子菜单
                .filter(m -> m.getParentId().equals(menu.getId()))
                // 递归为每个子菜单查找并设置其下级子菜单，构建完整的子树
                .map(m -> m.setChildren(getChildren(m, menuVoList)))
                // 收集所有子菜单并返回
                .collect(Collectors.toList());
    }
}