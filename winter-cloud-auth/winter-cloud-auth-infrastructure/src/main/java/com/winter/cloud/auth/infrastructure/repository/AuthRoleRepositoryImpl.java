package com.winter.cloud.auth.infrastructure.repository;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthRoleRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthRoleInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthRoleMenuPO;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthRoleMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMPService;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMenuMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserRoleMpService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;

@Repository
@RequiredArgsConstructor
public class AuthRoleRepositoryImpl implements AuthRoleRepository {
    private final IAuthUserRoleMpService authUserRoleMpService;
    private final IAuthRoleMPService authRoleMpService;
    private final IAuthRoleMenuMpService authRoleMenuMpService;
    private final AuthRoleMapper authRoleMapper;
    private final AuthRoleInfraAssembler authRoleInfraAssembler;
    @DubboReference
    public I18nMessageFacade i18nMessageFacade;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleSave(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), i18nMessageFacade.getMessage(CommonConstants.I18nKey.ROLE_NAME_OR_IDENTIFIER_EXISTS, LocaleContextHolder.getLocale()));
        }
        AuthRolePO authRolePO = authRoleInfraAssembler.toPO(authRoleDO);
        return authRoleMpService.save(authRolePO);
    }

    @Override
    public boolean hasDuplicateRole(AuthRoleDO authRoleDO) {
        LambdaQueryWrapper<AuthRolePO> authRolePOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        authRolePOLambdaQueryWrapper.nested(
                        e -> e.eq(AuthRolePO::getRoleName, authRoleDO.getRoleName())
                                .or()
                                .eq(AuthRolePO::getRoleKey, authRoleDO.getRoleKey()))
                .ne(ObjectUtil.isNotEmpty(authRoleDO.getId()), AuthRolePO::getId, authRoleDO.getId());
        long count = authRoleMpService.count(authRolePOLambdaQueryWrapper);
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleUpdate(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), i18nMessageFacade.getMessage(CommonConstants.I18nKey.ROLE_NAME_OR_IDENTIFIER_EXISTS, LocaleContextHolder.getLocale()));
        }
        AuthRolePO authRolePO = authRoleInfraAssembler.toPO(authRoleDO);
        return authRoleMpService.updateById(authRolePO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleDelete(List<Long> roleIds) {
        List<Long> allowDeleteRoleIds;
        // 根据传入的角色编号，查询用户角色关联的信息，如果没有，说明没有关联用户，可以删除，如果查询有结果，说明有关联用户，有关联的用户的角色不能删除，用现有的角色剔除有关联的用户角色，剩下的删除
        List<AuthUserRolePO> list = authUserRoleMpService.list(new LambdaQueryWrapper<AuthUserRolePO>().in(AuthUserRolePO::getRoleId, roleIds));
        if(ObjectUtil.isNotEmpty(list)){
            // 这是已经存在关联的角色，这是不能删除的，需要获取对应的用户编号集合并告知前端
            List<Long> collect = list.stream().map(AuthUserRolePO::getRoleId).collect(Collectors.toList());
            allowDeleteRoleIds = roleIds.stream().filter(roleId -> !collect.contains(roleId)).collect(Collectors.toList());
        }else{
            allowDeleteRoleIds = roleIds;
        }
        boolean b = authRoleMpService.removeByIds(allowDeleteRoleIds);
        boolean remove = authRoleMenuMpService.remove(new LambdaQueryWrapper<AuthRoleMenuPO>().in(AuthRoleMenuPO::getRoleId, allowDeleteRoleIds));
        return b && remove;
    }

    /**
     * 分页查询角色列表，并支持安全的字段排序（使用 Stream 流处理）。
     *
     * @param roleQuery 查询条件（包含分页参数、排序规则、搜索关键字等）
     * @return 分页结果，封装为 {@link PageDTO<RoleResponseDTO>}
     */
    @Override
    public PageDTO<AuthRoleDO> rolePage(RoleQuery roleQuery) {
        // 1. 构建分页对象
        Page<AuthUserPO> page = new Page<>(roleQuery.getPageNum(), roleQuery.getPageSize());
        IPage<AuthRolePO> rolePage = authRoleMapper.selectRolePage(page, roleQuery);
        List<AuthRoleDO> doList = authRoleInfraAssembler.toDOList(rolePage.getRecords());
        return new PageDTO<>(doList, rolePage.getTotal());
    }


    @Override
    public List<AuthRoleDO> selectRoleListByUserId(Long userId, String status) {
        if (ObjectUtil.isNotEmpty(userId)) {
            List<AuthRolePO> authRolePOList = authRoleMapper.selectRoleIdListByUserId(userId, status);
            return authRoleInfraAssembler.toDOList(authRolePOList);
        }
        return List.of();
    }

    @Override
    public List<AuthRoleDO> roleDynamicQueryList(RoleQuery roleQuery) {
        LambdaQueryWrapper<AuthRolePO> queryWrapper = new LambdaQueryWrapper<AuthRolePO>()
                .eq(ObjectUtil.isNotEmpty(roleQuery.getId()), AuthRolePO::getId, roleQuery.getId())
                .eq(ObjectUtil.isNotEmpty(roleQuery.getStatus()), AuthRolePO::getStatus, roleQuery.getStatus())
                .like(ObjectUtil.isNotEmpty(roleQuery.getRoleKey()), AuthRolePO::getRoleKey, roleQuery.getRoleKey())
                .like(ObjectUtil.isNotEmpty(roleQuery.getRoleName()), AuthRolePO::getRoleName, roleQuery.getRoleName());
        List<AuthRolePO> list = authRoleMpService.list(queryWrapper);
        return authRoleInfraAssembler.toDOList(list);
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignMenuPermissions(Long roleId, List<Long> menuIds) {
        //  删除之前角色拥有的资源
        authRoleMenuMpService.remove(new LambdaQueryWrapper<AuthRoleMenuPO>().eq(AuthRoleMenuPO::getRoleId, roleId));
        //  添加角色新的资源
        List<AuthRoleMenuPO> authRoleMenuPOList = menuIds.stream().map(menuId -> AuthRoleMenuPO.builder().roleId(roleId).menuId(menuId).build()).collect(Collectors.toList());
        authRoleMenuMpService.saveBatch(authRoleMenuPOList, 100);
    }
}
