package com.winter.cloud.auth.domain.repository;

import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;

import java.util.List;

public interface AuthDeptRepository {

    List<AuthDeptDO> deptDynamicQuery(DeptQuery deptQuery);

    List<AuthDeptDO> selectDeptListByUserId(Long userId, String status);
}
