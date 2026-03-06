package com.winter.cloud.auth.application.service;

import com.winter.cloud.auth.api.dto.command.UpsertDeptCommand;
import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface AuthDeptAppService {
    List<DeptResponseDTO> selectAllRecursionDept(DeptQuery deptQuery);

    List<DeptResponseDTO> deptTree(DeptQuery menuQuery);

    Boolean deptSave(UpsertDeptCommand command);

    Boolean deptUpdate(UpsertDeptCommand command);

    Boolean deptDelete(@NotNull Long id);
}
