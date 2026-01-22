package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.repository.AuthDeptRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthDeptInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthDeptMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthDeptMPService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuthDeptRepositoryImpl implements AuthDeptRepository {
    private final IAuthDeptMPService authDeptMPService;
    private final AuthDeptInfraAssembler authdeptInfraAssembler;
    private final AuthDeptMapper authDeptMapper;

    @Override
    public List<AuthDeptDO> deptDynamicQuery(DeptQuery deptQuery) {
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
}
