package com.winter.cloud.auth.infrastructure.repository;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthRoleRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthRoleInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthRoleMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMPService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;
import static com.winter.cloud.common.enums.ResultCodeEnum.ILLEGAL_PARAMETER;

@Repository
@RequiredArgsConstructor
public class AuthRoleRepositoryImpl implements AuthRoleRepository {

    private final IAuthRoleMPService authRoleMpService;
    private final AuthRoleMapper authRoleMapper;
    private final AuthRoleInfraAssembler authRoleInfraAssembler;

    @Override
    public Boolean saveRole(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), "角色名称/标识已存在！");
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

    @Override
    public Boolean updateRole(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), "角色名称/标识已存在！");
        }
        AuthRolePO authRolePO = authRoleInfraAssembler.toPO(authRoleDO);
        return authRoleMpService.updateById(authRolePO);
    }

    @Override
    public Boolean deleteRole(List<Long> roleIds) {
        // todo 查询该角色有没有关联用户，有的话就不能删除
        //todo 根据id数组批量删除角色信息
        //todo 根据id数组批量删除 角色菜单中间表的信息
        return true;
    }

    @Override
    /**
     * 分页查询角色列表，并支持安全的字段排序（使用 Stream 流处理）。
     *
     * @param roleQuery 查询条件（包含分页参数、排序规则、搜索关键字等）
     * @return 分页结果，封装为 {@link PageDTO<RoleResponseDTO>}
     */
    public PageDTO<AuthRoleDO> rolePage(RoleQuery roleQuery) {
        // 1. 初始化分页对象（防御性处理 null 值）
        long pageNum = roleQuery.getPageNum();
        long pageSize = roleQuery.getPageSize();
        Page<AuthRolePO> page = new Page<>(pageNum, pageSize);

        // 2. 安全校验并转换排序规则（使用 Stream 处理）
        List<PageAndOrderDTO.OrderDTO> orders = roleQuery.getOrders();
        if (ObjectUtil.isNotEmpty(orders)) {
            // 定义允许排序的字段白名单（Set 提升 contains 性能）
            Set<String> allowedFields = CollUtil.set(false, "create_time", "update_time", "role_sort");

            // 校验：是否存在非法字段（null 或不在白名单中）
            boolean hasIllegalField = orders.stream()
                    .map(PageAndOrderDTO.OrderDTO::getField)
                    .anyMatch(field -> ObjectUtil.isEmpty(field) || !allowedFields.contains(field));

            if (hasIllegalField) {
                throw new BusinessException(
                        ILLEGAL_PARAMETER.getCode(),
                        "排序字段非法，仅支持: create_time, update_time, role_sort"
                );
            }

            // 使用 Stream 将 OrderDTO 转换为 MyBatis-Plus 的 OrderItem，并添加到分页对象
            List<OrderItem> orderItems = orders.stream()
                    .map(order -> {
                        String field = order.getField();
                        String orderType = order.getOrder();
                        return CommonConstants.Order.DESC.equals(orderType)
                                ? OrderItem.desc(field)
                                : OrderItem.asc(field); // 默认 ASC
                    })
                    .collect(Collectors.toList());

            page.addOrder(orderItems); // MyBatis-Plus 支持批量添加 OrderItem
        }

        // 3. 执行数据库分页查询
        IPage<AuthRolePO> result = authRoleMapper.rolePage(page, roleQuery);

        // 4. 转换数据：PO 列表 → DO列表
        List<AuthRoleDO> doList = authRoleInfraAssembler.toDOList(result.getRecords());

        // 5. 构造并返回分页响应
        return new PageDTO<>(doList, result.getTotal());
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

}
