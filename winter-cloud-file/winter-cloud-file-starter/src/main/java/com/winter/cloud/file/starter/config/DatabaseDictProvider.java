package com.winter.cloud.file.starter.config;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.validation.provider.DictDataProvider;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DatabaseDictProvider implements DictDataProvider {

    /**
     * Dubbo 远程认证服务
     */
    @DubboReference(check = false)
    private DictFacade dictFacade;

    /**
     * Redis 操作模板
     */
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public Collection<String> getDictValues(String dictType, boolean reverse) {
        //  从redis中获取字典内容，没有在远程调用。字典微服务初始化时，会预先加载一份字典数据，下次查询时，会从缓存中获取。
        Object o = winterRedisTemplate.get(CommonConstants.Redis.DICT_KEY + CommonConstants.Redis.SPLIT + dictType);
        if (ObjectUtil.isNotEmpty(o)) {
            try {
                List<DictDataDTO> list = objectMapper.readValue(
                        o.toString(),
                        new TypeReference<List<DictDataDTO>>() {
                        }
                );
                if(reverse){
                    return list.stream().map(DictDataDTO::getDictLabel).collect(Collectors.toList());
                }
                return list.stream().map(DictDataDTO::getDictValue).collect(Collectors.toList());
            } catch (Exception e) {
                // JSON解析失败，继续从远程服务获取
            }
        }
        // 调用业务服务，根据字典类型查出所有字典键值
        Response<Map<String, List<DictDataDTO>>> dictDataByType = dictFacade.getDictDataByType(new DictCommand(Long.valueOf(dictType), "1"));
        // 注意处理空指针，建议返回空集合而不是 null
        // 如果 reverse 为 false，则返回字典值，否则返回字典标签
        if (reverse) {
            return dictDataByType.getData().values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(dictData -> dictData.getDictTypeId().toString().equals(dictType))
                    .map(DictDataDTO::getDictLabel)
                    .collect(Collectors.toList());
        }
        return dictDataByType.getData().values()
                .stream()
                .flatMap(List::stream)
                .filter(dictData -> dictData.getDictTypeId().toString().equals(dictType))
                .map(DictDataDTO::getDictValue)
                .collect(Collectors.toList());
    }
}