package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.application.service.AuthPostAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职位接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class AuthPostController {
    private final AuthPostAppService authPostAppService;
    private final WinterI18nTemplate winterI18nTemplate;


    /**
     * 根据状态和角色名称获取职位信息
     *
     * @param postName 职位名称
     * @param status   状态
     */
    @GetMapping("/getAllPostInfo")
    public Response<List<PostResponseDTO>> getAllRoleInfo(@RequestParam(value = "postName") String postName,
                                                          @RequestParam(value = "status") String status) {
        List<PostResponseDTO> data = authPostAppService.getAllPostInfo(postName, status);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }
}
