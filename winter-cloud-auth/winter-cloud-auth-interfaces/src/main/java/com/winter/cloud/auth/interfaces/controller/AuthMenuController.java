package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.application.service.AuthMenuAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜单
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Validated
public class AuthMenuController {
    private final AuthMenuAppService authMenuAppService;
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    /**
     * 获取动态路由(只需要查询对应的菜单类型为m的即可，不需要递归)
     *
     * @param id 用户ID
     * @return 菜单列表
     */
    @GetMapping("/getDynamicRouting")
    public Response<List<MenuResponseDTO>> getDynamicRouting(@NotNull @RequestParam(value = "id") Long id) {
        List<MenuResponseDTO> dynamicRouting = authMenuAppService.getDynamicRouting(id);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),dynamicRouting);
    }

}
