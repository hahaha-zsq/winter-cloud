package com.winter.cloud.auth.domain.repository;

import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;

import java.util.List;

public interface AuthDeptRepository {
    List<AuthDeptDO> selectAllDept(String deptName, String status);
}
