package com.winter.cloud.dict.interfaces.controller;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.winter.cloud.dict.application.service.DictAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/dict")
public class DictController implements DictFacade {

    private final DictAppService dictAppService;

    @PostMapping("/getDictDataByType")
    @Override
    public Response<Map<String,List<DictDataDTO>>> getDictDataByType(@RequestBody DictCommand dictCommand) {
        List<DictDataDTO> data = dictAppService.getDictDataByType(dictCommand.getDictType(), dictCommand.getStatus());
        Map<String, List<DictDataDTO>> collect = data.stream().collect(Collectors.groupingBy(DictDataDTO::getDictName));
        return Response.ok(collect);
    }
}
