package com.winter.cloud.auth.application.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.auth.application.service.IconAppService;
import com.winter.cloud.auth.domain.model.entity.IconTypeDO;
import com.winter.cloud.auth.domain.model.entity.IconValueDO;
import com.winter.cloud.auth.domain.repository.IconRepository;
import com.winter.cloud.common.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconAppServiceImpl implements IconAppService {

    private final ObjectMapper objectMapper;
    private final IconRepository iconRepository;


    @Override
    public void insert(String name) {
        try {
            String template = "https://icones.js.org/collections/{}-meta.json";
            String str = StrUtil.format(template, name.trim());
            String result = HttpUtil.get(str);
            // JSON -> Map
            Map<String, Object> info = objectMapper.readValue(
                    result,
                    new TypeReference<Map<String, Object>>() {}
            );
            // 图标名称
            String iconName = (String) info.get("name");
            // 图标前缀
            String prefix = (String) info.get("id");
            Map<String, Object> author = objectMapper.readValue(
                    objectMapper.writeValueAsString(info.get("author")),
                    new TypeReference<Map<String, Object>>() {}
            );
            // 项目地址
            String url = (String) author.get("url");
            // 图标信息
            List<String> iconList = objectMapper.readValue( objectMapper.writeValueAsString(info.get("icons")), new TypeReference<List<String>>() {
            });

            // 图标种类信息
            IconTypeDO iconTypeDO = IconTypeDO.builder()
                    .url(url)
                    .prefix(prefix)
                    .name(iconName)
                    .build();
            // 保存图标种类信息，无这个类型就新增，并返回种类编号，有也返回种类编号
            Long iconTypeId = iconRepository.saveIconType(iconTypeDO);

            // 图标信息
            List<IconValueDO> iconValueDOList = iconList.stream().map(icon -> {
                        return IconValueDO.builder()
                                .iconTypeId(iconTypeId)
                                .value(prefix + ":" + icon)
                                .status(StatusEnum.ENABLE.getCode()).build();
                    }
            ).collect(Collectors.toList());
            // 保存图标信息
            iconRepository.saveIconValue(iconValueDOList);
        } catch (Exception e) {
            log.error("读取 icon meta 失败", e);
        }
    }

    @Override
    public List<IconResponseDTO> getIconList(String name) {
        return iconRepository.getIconList(name);
    }
}