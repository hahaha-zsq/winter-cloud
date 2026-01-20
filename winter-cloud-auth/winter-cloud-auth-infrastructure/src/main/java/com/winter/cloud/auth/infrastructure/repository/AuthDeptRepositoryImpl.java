package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.repository.AuthDeptRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthDeptInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import com.winter.cloud.auth.infrastructure.service.IAuthDeptMPService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@RequiredArgsConstructor
public class AuthDeptRepositoryImpl implements AuthDeptRepository {
    private final IAuthDeptMPService authDeptMPService;
    private final AuthDeptInfraAssembler authdeptInfraAssembler;


    @Override
    public List<AuthDeptDO> selectAllDept(String deptName, String status) {
        LambdaQueryWrapper<AuthDeptPO> queryWrapper = new LambdaQueryWrapper<AuthDeptPO>()
                .like(AuthDeptPO::getDeptName, deptName)
                .eq(ObjectUtil.isNotEmpty(status), AuthDeptPO::getStatus, status);
        List<AuthDeptPO> list = authDeptMPService.list(queryWrapper);
        return authdeptInfraAssembler.toDOList(list);
    }
}
