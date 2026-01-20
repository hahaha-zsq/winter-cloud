package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.application.service.AuthDeptAppService;
import com.winter.cloud.auth.application.service.AuthMenuAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dept")
public class AuthDeptController {
    private final AuthDeptAppService authDeptAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 查询所有部门（递归）
     */
    @GetMapping("/selectAllRecursionDept")
    public Response<List<DeptResponseDTO>> selectAllRecursionDept(@RequestParam(value="deptName") String deptName,
                                                       @RequestParam(value = "status") String status) {
        List<DeptResponseDTO> data = authDeptAppService.selectAllRecursionDept(deptName,status);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);

    }
}
