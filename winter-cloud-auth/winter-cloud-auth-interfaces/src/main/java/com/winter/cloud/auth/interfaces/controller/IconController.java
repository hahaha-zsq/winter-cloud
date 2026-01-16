package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.auth.api.facade.IconFacade;
import com.winter.cloud.auth.application.service.IconAppService;
import com.winter.cloud.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/icon")
/*
* 图标控制器
* */
public class IconController implements IconFacade {
    private final IconAppService iconAppService;

    @GetMapping("/getIconList")
    @Override
    public Response<List<IconResponseDTO>> getIconList(String name) {
        List<IconResponseDTO> data=iconAppService.getIconList(name);
        return Response.ok(data);
    }
}
