package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.application.service.AuthPostAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
     * @param postQuery 职位查询条件
     */
    @PostMapping("/postDynamicQueryList")
    public Response<List<PostResponseDTO>> postDynamicQueryList(@RequestBody @Validated PostQuery postQuery) {
        List<PostResponseDTO> data = authPostAppService.postDynamicQueryList(postQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }
}
