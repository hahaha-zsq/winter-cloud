package com.winter.cloud.i18n.infrastructure.service.impl.excel;

import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.zsq.winter.office.entity.excel.WinterExcelDynamicSelect;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExcelSelectImpl implements WinterExcelDynamicSelect {
    @DubboReference(check = false)
    private DictFacade dictFacade;

    @Override
    public String[] getSource(String type) {
        Response<List<DictDataDTO>> listResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(Long.parseLong(type)).build());
        List<DictDataDTO> list = listResponse.getData();
        List<String> collect = list.stream().map(DictDataDTO::getDictLabel).collect(Collectors.toList());
        return collect.toArray(String[]::new);
    }
}
