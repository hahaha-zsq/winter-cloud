package com.winter.cloud.auth.domain.repository;

import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;

import java.util.List;

public interface AuthDeptRepository {

    List<AuthDeptDO> deptDynamicQueryList(DeptQuery deptQuery);

    List<AuthDeptDO> selectDeptListByUserId(Long userId, String status);

    Boolean deptSave(AuthDeptDO authDeptDO);

    Boolean deptUpdate(AuthDeptDO authDeptDO);

    Boolean deptDelete(Long id);

    List<AuthDeptDO> deptTree(DeptQuery menuQuery);
}
