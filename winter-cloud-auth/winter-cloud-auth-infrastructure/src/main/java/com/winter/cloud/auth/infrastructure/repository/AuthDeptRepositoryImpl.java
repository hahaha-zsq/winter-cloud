package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.repository.AuthDeptRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthDeptInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserDeptPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthDeptMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthDeptMPService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserDeptMpService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AuthDeptRepositoryImpl implements AuthDeptRepository {
    private final IAuthDeptMPService authDeptMPService;
    private final IAuthUserDeptMpService authUserDeptMpService;
    private final AuthDeptInfraAssembler authdeptInfraAssembler;
    private final AuthDeptMapper authDeptMapper;
    private final WinterI18nTemplate winterI18nTemplate;

    @Override
    public List<AuthDeptDO> deptDynamicQueryList(DeptQuery deptQuery) {
        LambdaQueryWrapper<AuthDeptPO> queryWrapper = new LambdaQueryWrapper<AuthDeptPO>()
                .like(ObjectUtil.isNotEmpty(deptQuery.getDeptName()), AuthDeptPO::getDeptName, deptQuery.getDeptName())
                .eq(ObjectUtil.isNotEmpty(deptQuery.getStatus()), AuthDeptPO::getStatus, deptQuery.getStatus())
                .eq(ObjectUtil.isNotEmpty(deptQuery.getId()), AuthDeptPO::getId, deptQuery.getId())
                .eq(ObjectUtil.isNotEmpty(deptQuery.getParentId()), AuthDeptPO::getParentId, deptQuery.getParentId());
        List<AuthDeptPO> list = authDeptMPService.list(queryWrapper);
        return authdeptInfraAssembler.toDOList(list);
    }

    @Override
    public List<AuthDeptDO> selectDeptListByUserId(Long userId, String status) {
        if (ObjectUtil.isNotEmpty(userId)) {
            List<AuthDeptPO> authDeptPOList = authDeptMapper.selectDeptListByUserId(userId, status);
            return authdeptInfraAssembler.toDOList(authDeptPOList);
        }
        return List.of();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deptSave(AuthDeptDO authDeptDO) {
        AuthDeptPO po = authdeptInfraAssembler.toPO(authDeptDO);
        return authDeptMPService.save(po);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deptUpdate(AuthDeptDO authDeptDO) {
        AuthDeptPO po = authdeptInfraAssembler.toPO(authDeptDO);
        return authDeptMPService.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deptDelete(Long id) {
        long count = authDeptMPService.count(new LambdaQueryWrapper<AuthDeptPO>().eq(AuthDeptPO::getParentId, id));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("Dept.delete.subdept.first"));
        }

        long userCount = authUserDeptMpService.count(new LambdaQueryWrapper<AuthUserDeptPO>().eq(AuthUserDeptPO::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("Dept.delete.user.bound"));
        }

        boolean isDeleted = authDeptMPService.removeById(id);
        if (!isDeleted) {
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "Dept deletion failed or dept does not exist");
        }

        return true;
    }

    @Override
    public List<AuthDeptDO> deptTree(DeptQuery menuQuery) {
        List<AuthDeptPO> allList = authDeptMPService.list(new LambdaQueryWrapper<AuthDeptPO>()
                .like(ObjectUtil.isNotEmpty(menuQuery.getDeptName()), AuthDeptPO::getDeptName, menuQuery.getDeptName())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getStatus()), AuthDeptPO::getStatus, menuQuery.getStatus())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getId()), AuthDeptPO::getId, menuQuery.getId())
                .eq(ObjectUtil.isNotEmpty(menuQuery.getParentId()), AuthDeptPO::getParentId, menuQuery.getParentId())
                .orderByAsc(AuthDeptPO::getOrderNum)
        );
        List<AuthDeptDO> allNodes = authdeptInfraAssembler.toDOList(allList);
        return buildTreeUseMap(allNodes);
    }

    private List<AuthDeptDO> buildTreeUseMap(List<AuthDeptDO> allNodes) {
        if (CollUtil.isEmpty(allNodes)) {
            return List.of();
        }

        Map<Long, AuthDeptDO> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(
                        AuthDeptDO::getId,
                        Function.identity(),
                        (oldVal, newVal) -> oldVal
                ));

        List<AuthDeptDO> rootNodes = new ArrayList<>();
        for (AuthDeptDO node : allNodes) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                rootNodes.add(node);
            } else {
                AuthDeptDO parent = nodeMap.get(parentId);
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(node);
            }
        }

        rootNodes.sort(Comparator.comparing(
                AuthDeptDO::getOrderNum,
                Comparator.nullsLast(Integer::compareTo)
        ));

        allNodes.forEach(node -> {
            if (CollUtil.isNotEmpty(node.getChildren())) {
                node.getChildren().sort(Comparator.comparing(
                        AuthDeptDO::getOrderNum,
                        Comparator.nullsLast(Integer::compareTo)
                ));
            }
        });

        return rootNodes;
    }
}
