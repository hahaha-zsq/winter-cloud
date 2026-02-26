package com.winter.cloud.auth.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.winter.cloud.auth.api.dto.command.UpsertMenuCommand;
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

import java.util.*;
import java.util.function.Function;
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
        // 1. 根据查询条件查询符合要求的菜单列表 (例如：名称模糊搜索)
        List<AuthMenuDO> matchList = authMenuRepository.getMenuList(menuQuery);

        if (CollUtil.isEmpty(matchList)) {
            return Collections.emptyList();
        }

        // 2. 收集所有涉及到的ID (包括自身ID和所有祖先ID) -> 使用 Set 自动去重
        Set<Long> allIds = new HashSet<>();
        for (AuthMenuDO menu : matchList) {
            // 2.1 收集自身 ID
            if (menu.getId() != null) {
                allIds.add(menu.getId());
            }
            // 2.2 解析 ancestors（如 "0,1,2"）
            String ancestors = menu.getAncestors();
            if (StrUtil.isBlank(ancestors)) {
                continue;
            }
            for (String ancestorId : ancestors.split(CommonConstants.Delimiter.ENGLISH_COMMA)) {
                // 跳过空值和根节点标识
                if (StrUtil.isBlank(ancestorId) || "0".equals(ancestorId)) {
                    continue;
                }
                try {
                    allIds.add(Long.parseLong(ancestorId));
                } catch (NumberFormatException e) {
                    log.error("Invalid ancestor id [{}] in menuId={}", ancestorId, menu.getId());
                }
            }
        }

        // 3. 根据ID集合一次性查询所有节点信息 (这是防止重复的关键步骤)
        // 假设 Repository 有 listByIds 方法，如果没有请在 Repository 中添加: List<AuthMenuDO> listByIds(Collection<Long> ids);
        // 通常 MyBatisPlus 的 Service 或 Mapper 自带 selectBatchIds
        List<AuthMenuDO> allNodesDO = authMenuRepository.listByIds(allIds);

        // 4. 转换 DO -> DTO
        List<MenuResponseDTO> allNodesDTO = authMenuAppAssembler.toDTOList(allNodesDO);

        // 5. 利用 Map 构建树形结构 (解决父节点重复分裂的问题)
        return buildTreeUseMap(allNodesDTO);
    }

    @Override
    public boolean menuSave(UpsertMenuCommand command) {
        AuthMenuDO authMenuDO = authMenuAppAssembler.toDO(command);
        return authMenuRepository.menuSave(authMenuDO);
    }

    @Override
    public List<Long> resourcesOwnedList(Long roleId) {
        return authMenuRepository.getResourcesListByRoleId(roleId);
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
     * @param menu       当前菜单节点，需要为其查找子菜单
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

    /**
     * 基于 Map 构建菜单树结构（推荐做法）
     *
     * <p>
     * 将一个“扁平化”的菜单节点列表（每个节点只包含 id / parentId）
     * 转换为真正的树形结构（父节点包含 children 子节点列表）。
     * </p>
     *
     * <h3>核心思路</h3>
     * <ol>
     *   <li>先将所有节点放入 Map（id -> node），实现 O(1) 父节点查找</li>
     *   <li>再次遍历节点，根据 parentId 将节点挂载到父节点的 children 中</li>
     *   <li>找不到父节点或 parentId = 0 的节点，视为根节点</li>
     * </ol>
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>菜单树（后台管理系统菜单）</li>
     *   <li>权限树 / 资源树</li>
     *   <li>组织架构树</li>
     *   <li>分类树（商品分类、文章分类）</li>
     * </ul>
     *
     * <h3>特点</h3>
     * <ul>
     *   <li>时间复杂度 O(n)，性能优于递归 + 多次遍历</li>
     *   <li>支持节点乱序输入</li>
     *   <li>可容忍“孤儿节点”（父节点不存在）</li>
     * </ul>
     *
     * @param allNodes 所有菜单节点的扁平列表（无层级关系）
     * @return 构建完成的树形结构根节点列表
     */
    private List<MenuResponseDTO> buildTreeUseMap(List<MenuResponseDTO> allNodes) {
        // 1. 将 List 转换为 Map<id, node>
        // key：节点 id
        // value：节点对象本身
        // 如果存在重复 id，只保留第一次出现的节点，后面的直接丢弃，避免 Duplicate key 异常
        // 这样做的目的是：后续可以通过 parentId O(1) 找到父节点
        Map<Long, MenuResponseDTO> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(
                        MenuResponseDTO::getId,          // Map 的 key：节点 id
                        Function.identity(),             // Map 的 value：节点对象本身
                        (oldVal, newVal) -> oldVal       // key 冲突时保留旧值
                ));
        // 用于存放最终返回的“根节点”列表
        List<MenuResponseDTO> rootNodes = new ArrayList<>();
        // 2. 再次遍历所有节点，构建父子关系
        for (MenuResponseDTO node : allNodes) {
            // 当前节点的父节点 ID
            Long parentId = node.getParentId();
            // 以下几种情况，将当前节点视为“根节点”：
            // 1) parentId == null
            // 2) parentId == 0（约定 0 为顶级节点）
            // 3) parentId 在 Map 中不存在（孤儿节点 / 数据不完整）
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                rootNodes.add(node);
            } else {
                // 通过 parentId 从 Map 中快速找到父节点
                MenuResponseDTO parent = nodeMap.get(parentId);
                // 如果父节点的 children 为空，先进行初始化
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                // 将当前节点加入父节点的 children 列表
                // 注意：parent 是 Map 中的引用对象，
                // 所以所有子节点都会正确挂载到同一个父节点实例上
                parent.getChildren().add(node);
            }
        }
        // 3. 对根节点进行排序（按 orderNum 升序，null 排在最后）
        rootNodes.sort(
                Comparator.comparing(
                        MenuResponseDTO::getOrderNum,
                        Comparator.nullsLast(Integer::compareTo)
                )
        );
        // 4. 对每个节点的 children 进行排序（如果存在子节点）
        allNodes.forEach(node -> {
            if (CollUtil.isNotEmpty(node.getChildren())) {
                node.getChildren().sort(
                        Comparator.comparing(
                                MenuResponseDTO::getOrderNum,
                                Comparator.nullsLast(Integer::compareTo)
                        )
                );
            }
        });
        // 返回最终构建好的树形结构
        return rootNodes;
    }
}