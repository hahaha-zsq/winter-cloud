package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.query.DeptQuery;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.auth.application.service.AuthDeptAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    /**
     * 查询所有部门（递归）
     */
    @PostMapping("/selectAllRecursionDept")
    public Response<List<DeptResponseDTO>> selectAllRecursionDept(@RequestBody @Validated DeptQuery deptQuery) {
        List<DeptResponseDTO> data = authDeptAppService.selectAllRecursionDept(deptQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);

    }
}
