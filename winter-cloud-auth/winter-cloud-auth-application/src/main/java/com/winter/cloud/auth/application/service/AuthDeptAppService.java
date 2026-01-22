package com.winter.cloud.auth.application.service;

import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;

import java.util.List;

public interface AuthDeptAppService {
    List<DeptResponseDTO> selectAllRecursionDept(DeptQuery deptQuery);
}
