package com.winter.cloud.dict.api.facade;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;

import java.util.List;
import java.util.Map;

/**
 * 字典服务 RPC 接口定义
 * 包含 Spring MVC 注解，供 Feign 客户端使用
 * 
 * @author zsq
 */
public interface DictFacade {
    
    Response<Map<String,List<DictDataDTO>>> getDictDataByType(DictCommand command);
    Response<List<DictDataDTO>> dictValueDynamicQueryList(DictDataQuery dictQuery);
}
